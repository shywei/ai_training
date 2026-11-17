package com.example.agent.adapter.springai2;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Spring AI 2.0.2 GA Schema 自愈纠错拦截器 (SchemaRepairCallAdvisor)
 *
 * 实现 CallAdvisor 接口：
 * 当工具参数未通过 Jakarta Bean Validation 校验抛出 ConstraintViolationException 时，
 * 捕获违规字段明细并动态拼接 Repair Prompt，在最大重试次数内驱动模型重新生成合法参数。
 */
@Component
public class SchemaRepairCallAdvisor implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger(SchemaRepairCallAdvisor.class);

    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 200;
    private static final int MAX_REPAIR_ATTEMPTS = 2;

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        int attempt = 0;
        ChatClientRequest currentRequest = request;

        while (attempt <= MAX_REPAIR_ATTEMPTS) {
            try {
                return chain.nextCall(currentRequest);
            } catch (ConstraintViolationException cve) {
                attempt++;
                String errorDetails = cve.getConstraintViolations().stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining("; "));

                log.warn("[SchemaRepair] ⚠️ 触发 Bean Validation 校验异常 (第 {}/{} 次自愈): {}",
                        attempt, MAX_REPAIR_ATTEMPTS, errorDetails);

                if (attempt > MAX_REPAIR_ATTEMPTS) {
                    log.error("[SchemaRepair] ❌ 自愈重试次数耗尽，向上抛出校验异常");
                    throw cve;
                }

                String repairPrompt = String.format("""
                        [系统自愈纠错提示]
                        你上一轮调用的工具参数未通过企业校验契约，具体错误如下：
                        %s
                        请严格遵守数据模型规范（如商品ID格式为 NB001/PH001、购买数量大于0），重新生成正确的参数！
                        """, errorDetails);

                Prompt updatedPrompt = currentRequest.prompt().augmentUserMessage(
                        old -> new UserMessage(old.getText() + "\n\n" + repairPrompt)
                );
                currentRequest = currentRequest.mutate()
                        .prompt(updatedPrompt)
                        .context("repair_attempt", attempt)
                        .context("last_validation_error", errorDetails)
                        .build();

            } catch (IllegalArgumentException iae) {
                attempt++;
                log.warn("[SchemaRepair] ⚠️ 捕获非法参数异常 (第 {}/{} 次自愈): {}",
                        attempt, MAX_REPAIR_ATTEMPTS, iae.getMessage());

                if (attempt > MAX_REPAIR_ATTEMPTS) {
                    throw iae;
                }

                String repairPrompt = String.format("""
                        [系统自愈纠错提示]
                        参数格式解析失败: %s。请返回严格符合标准的 JSON 参数格式。
                        """, iae.getMessage());

                Prompt updatedPrompt = currentRequest.prompt().augmentUserMessage(
                        old -> new UserMessage(old.getText() + "\n\n" + repairPrompt)
                );
                currentRequest = currentRequest.mutate()
                        .prompt(updatedPrompt)
                        .context("repair_attempt", attempt)
                        .build();
            }
        }

        throw new IllegalStateException("Schema 自愈纠错循环异常终止");
    }
}
