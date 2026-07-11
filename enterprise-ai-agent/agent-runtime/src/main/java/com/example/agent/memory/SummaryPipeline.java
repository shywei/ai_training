package com.example.agent.memory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Summary Pipeline — 后台摘要压缩
 *
 * 压缩策略：
 * - 阈值：超过 8 条消息触发
 * - 压缩后：1 条累积摘要 + 4 条最近原文 = 5 条
 * - 累积摘要：新摘要 = 旧摘要 + 旧原文，不会丢失历史信息
 *
 * 演示流程：
 *   第1轮：消息积累到 8 条
 *   第2轮：触发压缩 → 摘要A + 4条原文 = 5条
 *   第3轮：继续积累到 8 条（摘要A + 4条原文 + 3条新消息）
 *   第4轮：触发压缩 → 摘要B(含摘要A信息) + 4条最新原文 = 5条
 */
@Component
public class SummaryPipeline {

    private static final Logger log = LoggerFactory.getLogger("LLM_SUMMARY");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final SessionMemory sessionMemory;
    private final ChatModel chatModel;
    private final String summaryModel;
    private static final int SUMMARIZE_THRESHOLD = 8;
    private static final int KEEP_RECENT = 4;

    public SummaryPipeline(SessionMemory sessionMemory, ChatModel chatModel,
                           @Value("${agent.model.summary}") String summaryModel) {
        this.sessionMemory = sessionMemory;
        this.chatModel = chatModel;
        this.summaryModel = summaryModel;
    }

    /**
     * 判断是否需要摘要
     */
    public boolean needsSummarize(String sessionId) {
        return sessionMemory.getMessageCount(sessionId) > SUMMARIZE_THRESHOLD;
    }

    /**
     * 执行摘要压缩
     * 策略：把前 N-K 条（含旧摘要）压缩为新的累积摘要，保留最近 K 条原文
     */
    public String summarizeSession(String sessionId) {
        List<String> messages = sessionMemory.getMessages(sessionId);
        if (messages == null || messages.size() <= SUMMARIZE_THRESHOLD) {
            return null;
        }

        // 取出需要压缩的部分（前 N-K 条，可能包含旧摘要）
        int summarizeEnd = messages.size() - KEEP_RECENT;
        List<String> toSummarize = messages.subList(0, summarizeEnd);

        // 构建摘要 Prompt
        StringBuilder conversation = new StringBuilder();
        boolean hasOldSummary = false;
        for (String msg : toSummarize) {
            String[] parts = msg.split("\\|", 2);
            if (parts.length == 2) {
                String content = parts[1].replace("\\n", "\n");
                if (content.startsWith("[对话摘要]")) {
                    hasOldSummary = true;
                    conversation.append("之前的摘要: ").append(content.substring(6)).append("\n\n");
                } else {
                    String role = "user".equals(parts[0]) ? "用户" : "助手";
                    conversation.append(role).append(": ").append(content).append("\n");
                }
            }
        }

        String promptInstruction = hasOldSummary
                ? "之前已经有一份摘要，现在需要将旧摘要和新对话合并为一份更新的摘要。保留所有关键信息，不超过300字。"
                : "将以下对话压缩为简短摘要，保留关键信息（用户偏好、重要决策、商品信息），不超过300字。";

        String summaryPrompt = promptInstruction + "\n\n" + conversation;

        // 用快模型做摘要
        log.info("\n╔══════════════════════════════════════════════════╗\n║ SUMMARY REQUEST  {} ║\n╚══════════════════════════════════════════════════╝\n[Model] {}\n[Session] {}\n[Strategy] {}\n\n[Prompt]\n{}", LocalDateTime.now().format(FMT), summaryModel, sessionId, hasOldSummary ? "累积压缩（合并旧摘要）" : "首次压缩", summaryPrompt);

        long start = System.currentTimeMillis();
        String summary = ChatClient.create(chatModel)
                .prompt()
                .system("你是一个对话摘要助手。保留所有关键信息，不要遗漏。只输出摘要内容，不要加前缀。")
                .user(summaryPrompt)
                .options(OpenAiChatOptions.builder()
                        .withModel(summaryModel)
                        .withTemperature(0.1)
                        .build())
                .call()
                .content();
        long duration = System.currentTimeMillis() - start;

        log.info("\n╔══════════════════════════════════════════════════╗\n║ SUMMARY RESPONSE {} ║\n╚══════════════════════════════════════════════════╝\n[Model] {}\n[Duration] {}ms\n\n[Summary]\n{}", LocalDateTime.now().format(FMT), summaryModel, duration, summary);

        // 清空旧消息，写入新摘要 + 保留的最近消息
        sessionMemory.clear(sessionId);
        sessionMemory.addMessage(sessionId, "system", "[对话摘要] " + summary.trim());

        for (int i = summarizeEnd; i < messages.size(); i++) {
            String[] parts = messages.get(i).split("\\|", 2);
            if (parts.length == 2) {
                sessionMemory.addMessage(sessionId, parts[0], parts[1].replace("\\n", "\n"));
            }
        }

        System.out.println("[Summary] Session " + sessionId + " 压缩完成: "
                + toSummarize.size() + "条 → 1条摘要 + " + KEEP_RECENT + "条原文");

        return summary;
    }
}
