package com.example.agent.adapter.agentscope;

import com.example.agent.domain.AgentCheckpoint;
import com.example.agent.domain.ShoppingDomainService;
import com.example.agent.domain.ToolOrderRequest;
import com.example.agent.domain.WorkflowState;
import com.example.agent.port.CheckpointStorePort;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.middleware.ActingInput;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ModelCallInput;
import io.agentscope.core.middleware.ReasoningInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;

/**
 * AgentScope 2.0 五阶段生命周期扩展中间件（Lifecycle Middleware）
 *
 * 对应第 09 课 P11、P13、P14（拦截与扩展机制哲学对比）：
 *
 * ────────── 核心架构哲学差异对比 ──────────
 * 1. 【Spring AI 2.0 Advisors 机制】：
 *    - 核心抽象：CallAdvisor / StreamAdvisor。
 *    - 设计模型：经典请求/响应拦截责任链（类似 Servlet Filter 或 Spring MVC HandlerInterceptor）。
 *    - 拦截粒度：仅面向单次模型 HTTP 请求/响应（Single Model Request/Response），无法原生感知智能体
 *      的多轮 ReAct 思考步进、工具调用前后语义、或人在回路的会话挂起。
 *
 * 2. 【AgentScope 2.0 Middleware 机制】：
 *    - 核心抽象：MiddlewareBase。
 *    - 设计模型：原生嵌入智能体认知生命周期的五阶段扩展钩子（Hook）：
 *      ① onAgent                      (智能体任务入站与收官)
 *      ② onSystemPrompt               (系统提示词动态增强)
 *      ③ onModelCall                  (底层模型网络调用与 Token 计量)
 *      ④ onReasoning                  (大模型思考规划与推演前后)
 *      ⑤ onActing                     (工具调用拦截、风控拦截与结果脱敏，结合 .onErrorResume() 实现自愈)
 *    - 拦截粒度：贯穿智能体全局认知拓扑，可随时在工具执行前拦截并注入 RequireUserConfirmEvent 挂起任务！
 */
@Component
public class LifecycleMiddleware implements MiddlewareBase {

    private static final Logger log = LoggerFactory.getLogger(LifecycleMiddleware.class);

    private final ShoppingDomainService domainService;
    private final CheckpointStorePort checkpointStore;

    public LifecycleMiddleware(ShoppingDomainService domainService, CheckpointStorePort checkpointStore) {
        this.domainService = domainService;
        this.checkpointStore = checkpointStore;
    }

    @Override
    public Flux<AgentEvent> onAgent(Agent agent, RuntimeContext context, AgentInput input,
                                    Function<AgentInput, Flux<AgentEvent>> next) {
        String executionId = context.getSessionId() != null ? context.getSessionId() : "exec-" + UUID.randomUUID();
        log.info("[Middleware:onAgent:Start] 智能体启动任务, executionId={}, agent={}", executionId, agent.getName());

        long startTime = System.currentTimeMillis();

        return next.apply(input)
                .doOnComplete(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("[Middleware:onAgent:End] 智能体任务完成, executionId={}, 耗时={}ms", executionId, duration);
                })
                .doOnError(e -> {
                    log.error("[Middleware:onAgent:Error] 智能体执行发生异常: {}", e.getMessage(), e);
                });
    }

    @Override
    public Flux<AgentEvent> onReasoning(Agent agent, RuntimeContext context, ReasoningInput input,
                                        Function<ReasoningInput, Flux<AgentEvent>> next) {
        log.debug("[Middleware:onReasoning:Pre] 准备进入模型思考阶段, 历史消息数={}", input.messages().size());

        return next.apply(input)
                .doOnNext(event -> {
                    if (event.getType() == AgentEventType.THINKING_BLOCK_START) {
                        log.debug("[Middleware:onReasoning] 模型开始输出思维链 (Thinking Chain)...");
                    }
                });
    }

    @Override
    public Flux<AgentEvent> onActing(Agent agent, RuntimeContext context, ActingInput input,
                                     Function<ActingInput, Flux<AgentEvent>> next) {
        List<ToolUseBlock> toolCalls = input.toolCalls();
        if (toolCalls == null || toolCalls.isEmpty()) {
            return next.apply(input);
        }

        for (ToolUseBlock toolCall : toolCalls) {
            String toolName = toolCall.getName();
            Map<String, Object> toolInput = toolCall.getInput();
            log.info("[Middleware:onActing:PreToolCall] 拦截工具调用: toolName={}, input={}", toolName, toolInput);

            // 业务风控校验：高风险动作拦截（如创建订单且金额超阈值）
            ToolOrderRequest orderReq = extractOrderRequest(toolInput);
            if (domainService.isHighRiskAction(toolName, orderReq)) {
                log.warn("[Middleware:onActing:HITL] 触发业务硬风控拦截！工具={}, 下单金额={}",
                        toolName, orderReq.getTotalAmount());

                String executionId = context.getSessionId();
                String checkpointId = "cp-hitl-" + UUID.randomUUID().toString().substring(0, 8);

                // 记录挂起快照到 Checkpointer（P07~P10）
                AgentCheckpoint checkpoint = new AgentCheckpoint(executionId, checkpointId, null, WorkflowState.CONFIRMING, 1);
                checkpoint.setSessionId(executionId);
                checkpoint.setUserId(context.getUserId());
                Map<String, Object> pendingMeta = new HashMap<>();
                pendingMeta.put("toolName", toolName);
                pendingMeta.put("arguments", toolInput);
                pendingMeta.put("amount", orderReq.getTotalAmount());
                pendingMeta.put("riskReason", "订单金额 ¥" + orderReq.getTotalAmount() + " 超过高风险免密阈值 ¥" + ShoppingDomainService.HIGH_RISK_AMOUNT_THRESHOLD);
                checkpoint.setPendingConfirmation(pendingMeta);

                checkpointStore.saveSnapshot(checkpoint);
                context.put("APPROVAL_REQUIRED", Boolean.TRUE);
                context.put("LAST_CHECKPOINT_ID", checkpointId);

                // 发射 AgentScope 原生 RequireUserConfirmEvent 事件挂起任务
                RequireUserConfirmEvent confirmEvent = new RequireUserConfirmEvent(
                        checkpointId,
                        List.of(toolCall)
                );
                confirmEvent.withMetadata(pendingMeta);

                log.info("[Middleware:onActing] 成功发射 RequireUserConfirmEvent，执行挂起");
                return Flux.just(confirmEvent);
            }
        }

        // 风控通过，继续放行给工具适配器执行
        return next.apply(input)
                .doOnNext(event -> {
                    if (event.getType() == AgentEventType.TOOL_RESULT_END) {
                        log.info("[Middleware:onActing:PostToolCall] 工具执行完毕，结果已返回");
                    }
                });
    }

    @Override
    public Flux<AgentEvent> onModelCall(Agent agent, RuntimeContext context, ModelCallInput input,
                                        Function<ModelCallInput, Flux<AgentEvent>> next) {
        return next.apply(input);
    }

    @Override
    public Mono<String> onSystemPrompt(Agent agent, RuntimeContext context, String defaultSysPrompt) {
        String enhanced = defaultSysPrompt + "\n[System Notice] 当前运行于 AgentScope 2.0 企业安全沙箱，全量工具调用均受 Middleware 动态审计。";
        return Mono.just(enhanced);
    }

    private ToolOrderRequest extractOrderRequest(Map<String, Object> input) {
        if (input == null) return new ToolOrderRequest();
        String userId = Objects.toString(input.get("userId"), "U001");
        String productId = Objects.toString(input.get("productId"), "NB001");
        Integer quantity = input.get("quantity") instanceof Number n ? n.intValue() : 1;
        String address = Objects.toString(input.get("shippingAddress"), "北京市海淀区中关村南大街1号院");
        String phone = Objects.toString(input.get("contactPhone"), "13800138000");
        String idemKey = Objects.toString(input.get("idempotencyKey"), "IDEM-12345678");

        BigDecimal totalAmount = new BigDecimal("8999.00");
        if (input.containsKey("totalAmount")) {
            totalAmount = new BigDecimal(input.get("totalAmount").toString());
        }

        return new ToolOrderRequest(userId, productId, quantity, totalAmount, address, phone, idemKey);
    }
}
