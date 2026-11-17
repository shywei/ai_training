package com.example.agent.adapter.agentscope;

import com.example.agent.domain.WorkflowStreamEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.event.*;
import io.agentscope.core.message.ToolUseBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AgentScope 2.0 类型化事件流桥接器（Typed AgentEvent Stream Bridge）
 *
 * 对应第 09 课 P13~P14（AgentScope 28 类类型化 AgentEvent 与多通道解耦）：
 *
 * ────────── 核心杀手锏对比 ──────────
 * 1. 【Spring AI 2.0 的流式局限】：
 *    - 仅提供单一维度的文本 Token 流（Flux<ChatResponse>），将思考过程、工具调用与最终回答全部混杂
 *      在一个文本流中。
 *    - 在手搓 v5.0 中甚至必须用延迟发射拆字符串的方式伪造流式输出。
 *
 * 2. 【AgentScope 2.0 的 28 类类型化事件流（streamEvents）】：
 *    - 独立于拦截机制的原生反应式流输出能力。
 *    - 原生定义了覆盖全生命周期的 28 类类型化 AgentEvent（如 ThinkingBlockDeltaEvent, ToolCallStartEvent,
 *      RequireUserConfirmEvent, TextBlockDeltaEvent 等）。
 *    - 本类将 AgentScope 原生 28 类 AgentEvent 映射转换为统一的六边形领域事件 WorkflowStreamEvent，
 *      让前端能够实时分通道渲染思考链、工具执行卡片与打字机输出！
 */
@Component
public class AgentEventStreamBridge {

    private static final Logger log = LoggerFactory.getLogger(AgentEventStreamBridge.class);

    private final ObjectMapper objectMapper;

    public AgentEventStreamBridge(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将 AgentScope 原生 Flux<AgentEvent> 桥接并转换为领域统一的 WorkflowStreamEvent 反应式流
     *
     * @param executionId 任务执行全局 ID
     * @param eventFlux   AgentScope 2.0 原生类型化事件流
     * @return 领域事件流 Flux<WorkflowStreamEvent>
     */
    public Flux<WorkflowStreamEvent> bridgeToWorkflowStream(String executionId, Flux<AgentEvent> eventFlux) {
        return eventFlux.map(event -> transformEvent(executionId, event));
    }

    /**
     * 将事件流转换为 SSE 传输用的 JSON 字符串流
     */
    public Flux<String> bridgeToJsonStream(String executionId, Flux<AgentEvent> eventFlux) {
        return bridgeToWorkflowStream(executionId, eventFlux).map(streamEvent -> {
            try {
                return objectMapper.writeValueAsString(streamEvent);
            } catch (Exception e) {
                return "{\"type\":\"error\",\"executionId\":\"" + executionId + "\",\"payload\":\"" + e.getMessage() + "\"}";
            }
        });
    }

    private WorkflowStreamEvent transformEvent(String executionId, AgentEvent event) {
        AgentEventType type = event.getType();

        switch (type) {
            // ── 通道 1：思考过程流 (Thought Channel) ──
            case THINKING_BLOCK_START -> {
                return WorkflowStreamEvent.of("thought_start", executionId, "模型开始进行逻辑推演...");
            }
            case THINKING_BLOCK_DELTA -> {
                String delta = (event instanceof ThinkingBlockDeltaEvent de) ? de.getDelta() : "";
                return WorkflowStreamEvent.thought(executionId, 1, delta);
            }
            case THINKING_BLOCK_END -> {
                return WorkflowStreamEvent.of("thought_end", executionId, "推演完毕，准备执行动作");
            }

            // ── 通道 2：工具调用与执行流 (Tool Execution Channel) ──
            case TOOL_CALL_START -> {
                String toolName = (event instanceof ToolCallStartEvent se) ? se.getToolCallName() : "tool";
                return WorkflowStreamEvent.toolStart(executionId, 1, toolName, "开始调用工具: " + toolName);
            }
            case TOOL_CALL_DELTA -> {
                String delta = (event instanceof ToolCallDeltaEvent te) ? te.getDelta() : "";
                return WorkflowStreamEvent.of("tool_delta", executionId, delta);
            }
            case TOOL_CALL_END -> {
                return WorkflowStreamEvent.of("tool_call_end", executionId, "工具入参解析就绪");
            }
            case TOOL_RESULT_START, TOOL_RESULT_TEXT_DELTA, TOOL_RESULT_DATA_DELTA, TOOL_RESULT_END -> {
                return WorkflowStreamEvent.toolEnd(executionId, 1, "tool", "工具执行结果已返回");
            }

            // ── 通道 3：文本最终回答流 (Answer Channel) ──
            case TEXT_BLOCK_START -> {
                return WorkflowStreamEvent.of("text_start", executionId, "开始输出回复");
            }
            case TEXT_BLOCK_DELTA -> {
                String delta = (event instanceof TextBlockDeltaEvent te) ? te.getDelta() : "";
                return WorkflowStreamEvent.of("token", executionId, delta);
            }
            case TEXT_BLOCK_END -> {
                return WorkflowStreamEvent.of("text_end", executionId, "回复输出完毕");
            }

            // ── 通道 4：人在回路与审批流 (HITL Channel) ──
            case REQUIRE_USER_CONFIRM -> {
                WorkflowStreamEvent confirmEvent = WorkflowStreamEvent.of("confirm_required", executionId, "检测到大额高风险动作，需要人工审批确认");
                if (event instanceof RequireUserConfirmEvent ce) {
                    List<ToolUseBlock> calls = ce.getToolCalls();
                    if (calls != null && !calls.isEmpty()) {
                        confirmEvent.setPayload(ce.getMetadata() != null ? ce.getMetadata() : calls.get(0).getInput());
                    }
                }
                return confirmEvent;
            }

            // ── 通道 5：任务状态与收官控制流 ──
            case AGENT_START -> {
                return WorkflowStreamEvent.of("step_start", executionId, "智能体任务启动");
            }
            case AGENT_END, AGENT_RESULT -> {
                return WorkflowStreamEvent.completed(executionId, "智能体任务执行完成");
            }
            case CUSTOM -> {
                String customName = (event instanceof CustomEvent ce) ? ce.getName() : "custom";
                return WorkflowStreamEvent.of("schema_repair", executionId, "模式自愈触发: " + customName);
            }
            default -> {
                return WorkflowStreamEvent.of(type.name().toLowerCase(), executionId, event.toString());
            }
        }
    }
}
