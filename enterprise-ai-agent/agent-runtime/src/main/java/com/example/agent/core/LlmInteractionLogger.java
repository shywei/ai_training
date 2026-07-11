package com.example.agent.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * LLM 交互日志记录器
 * 将每次请求/响应写入独立日志文件 logs/llm-interactions.log
 *
 * 教学价值：学员能直观看到每次调用的完整上下文
 */
@Component
public class LlmInteractionLogger {

    private static final Logger log = LoggerFactory.getLogger("LLM_CHAT");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 记录 LLM 请求
     */
    public void logRequest(String model, String systemPrompt, String userPrompt) {
        log.info("""

                ╔══════════════════════════════════════════════════════════╗
                ║ LLM REQUEST  {} ║
                ╚══════════════════════════════════════════════════════════╝
                [Model] {}

                [System Prompt]
                {}

                [User Prompt]
                {}

                """,
                LocalDateTime.now().format(FMT),
                model,
                systemPrompt,
                userPrompt);
    }

    /**
     * 记录 LLM 响应
     */
    public void logResponse(String model, String response, long durationMs) {
        log.info("""

                ╔══════════════════════════════════════════════════════════╗
                ║ LLM RESPONSE {} ║
                ╚══════════════════════════════════════════════════════════╝
                [Model] {}
                [Duration] {}ms

                [Response]
                {}

                """,
                LocalDateTime.now().format(FMT),
                model,
                durationMs,
                response);
    }

    /**
     * 记录 Memory 注入情况
     */
    public void logMemoryInjection(String sessionId, String context) {
        log.info("""

                [Memory Injection] sessionId={}
                {}

                """,
                sessionId,
                context != null ? context : "(无上下文，context=null)");
    }

}
