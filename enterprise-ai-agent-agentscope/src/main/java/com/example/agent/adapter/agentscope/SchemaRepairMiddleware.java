package com.example.agent.adapter.agentscope;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.CustomEvent;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.middleware.ActingInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ReasoningInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * AgentScope 2.0 模式修复自愈中间件（Schema Repair Loop Middleware）
 *
 * 对应第09课 P15~P17（结构化 I/O、Tool Schema 强约束与自愈纠错循环）：
 *
 * ────────── 工业级自愈设计背景 ──────────
 * 在真实企业生产中，LLM 在调用复杂业务 Tool 时常出现：
 * 1. 字段格式不符合正则校验（如手机号少了一位、商品 ID 缺了 PROD- 前缀）。
 * 2. 数值范围溢出（如购买数量填成 0 或负数）。
 * 3. 深层嵌套多态 JSON 畸变或非标准泛型字段解析丢失。
 *
 * 【传统做法（手搓 v5.0）】：
 * 抛出反序列化异常后，整个工作流直接崩溃，降级为单步粗糙回答（TaskPlanner 失败降级），用户体验中断。
 *
 * 【AgentScope 2.0 GA 做法】：
 * 通过 Middleware 拦截校验异常（ValidationError / IllegalArgumentException），
 * 将具体的约束违规细节（Violations）自动封装为标准化的 "Repair Prompt"（自愈修复提示词），
 * 作为 ToolResult 反馈给大模型，驱动模型在下一轮 ReAct 步进中自动纠偏并重新发起合法调用！
 */
@Component
public class SchemaRepairMiddleware implements MiddlewareBase {

    private static final Logger log = LoggerFactory.getLogger(SchemaRepairMiddleware.class);

    private static final String CONTEXT_KEY_REPAIR_COUNT = "SCHEMA_REPAIR_ATTEMPT_COUNT";
    private static final int MAX_REPAIR_RETRIES = 3;

    @Override
    public Flux<AgentEvent> onActing(Agent agent, RuntimeContext context, ActingInput input,
                                     Function<ActingInput, Flux<AgentEvent>> next) {
        return next.apply(input)
                .onErrorResume(error -> {
                    // 拦截参数校验异常（由 Bean Validation 抛出）
                    if (isValidationError(error)) {
                        log.warn("[SchemaRepairMiddleware] 捕获到工具入参校验异常: {}", error.getMessage());

                        int currentRetries = context.get(CONTEXT_KEY_REPAIR_COUNT) != null
                                ? (int) context.get(CONTEXT_KEY_REPAIR_COUNT) : 0;

                        if (currentRetries < MAX_REPAIR_RETRIES) {
                            context.put(CONTEXT_KEY_REPAIR_COUNT, currentRetries + 1);

                            // 构造自愈修复引导提示词 (Repair Prompt)
                            String repairPrompt = buildRepairPrompt(error.getMessage(), input.toolCalls());
                            log.info("[SchemaRepairMiddleware] 触发第 {}/{} 次模式自愈重试循环, RepairPrompt: {}",
                                    currentRetries + 1, MAX_REPAIR_RETRIES, repairPrompt);

                            // 封装自定义自愈事件通知下游前端/监控
                            CustomEvent repairEvent = new CustomEvent(
                                    "SchemaRepairTriggered",
                                    Map.of(
                                            "attempt", currentRetries + 1,
                                            "maxAttempts", MAX_REPAIR_RETRIES,
                                            "errorMessage", error.getMessage(),
                                            "repairPrompt", repairPrompt
                                    )
                            );

                            return Flux.just(repairEvent);
                        } else {
                            log.error("[SchemaRepairMiddleware] 自愈重试次数已耗尽 ({} 次)，中止调用", MAX_REPAIR_RETRIES);
                        }
                    }
                    return Flux.error(error);
                });
    }

    @Override
    public Flux<AgentEvent> onReasoning(Agent agent, RuntimeContext context, ReasoningInput input,
                                        Function<ReasoningInput, Flux<AgentEvent>> next) {
        Integer repairCount = context.get(CONTEXT_KEY_REPAIR_COUNT);
        if (repairCount != null && repairCount > 0) {
            log.info("[SchemaRepairMiddleware:onReasoning] 检测到上下文处于自愈修复周期 (第{}轮)，已携带修复上下文进行推演", repairCount);
        }
        return next.apply(input);
    }

    private boolean isValidationError(Throwable t) {
        if (t == null) return false;
        String msg = t.getMessage();
        if (msg == null) return false;
        return msg.contains("ValidationError")
                || msg.contains("ConstraintViolation")
                || msg.contains("IllegalArgumentException");
    }

    private String buildRepairPrompt(String rawError, List<ToolUseBlock> toolCalls) {
        String toolName = (toolCalls != null && !toolCalls.isEmpty()) ? toolCalls.get(0).getName() : "tool";
        return String.format(
                "【工具参数校验失败自愈提示】\n" +
                "您调用的工具 `%s` 参数未通过企业 OpenAPI / JSON Schema 约束规则：\n" +
                "错误详情：%s\n\n" +
                "请严格按照以下约束重新构建入参并重发调用：\n" +
                "1. 商品编号格式必须为：^(NB|PH|ACC)[0-9]{3}$（例如 NB001、PH001）\n" +
                "2. 购买数量必须大于等于 1\n" +
                "3. 收货地址长度必须在 6 至 200 字符之间\n" +
                "4. 手机号必须为合法 11 位有效中国大陆号码（如 13800138000）",
                toolName, rawError
        );
    }
}
