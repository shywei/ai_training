package com.example.agent;

import com.example.agent.adapter.agentscope.AgentEventStreamBridge;
import com.example.agent.adapter.agentscope.AgentScopeCheckpointStore;
import com.example.agent.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.event.*;
import io.agentscope.core.message.ToolUseBlock;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 第 09 课《从手搓到框架：框架的边界》- 六边形架构与 AgentScope 2.0 核心能力单元测试
 */
class AgentScopeArchitectureTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ShoppingDomainService domainService = new ShoppingDomainService();

    @Test
    @DisplayName("P15: Jakarta Bean Validation 强约束校验测试 - 拦截非法入参并检验规则")
    void testToolOrderRequestValidation() {
        // 1. 测试非法入参：非法用户ID、非法商品ID、数量为0、手机号位数不足
        ToolOrderRequest invalidRequest = new ToolOrderRequest("USER_BAD", "INVALID_ID", 0, BigDecimal.ZERO, "短", "12345", "");
        Set<ConstraintViolation<ToolOrderRequest>> violations = validator.validate(invalidRequest);

        assertFalse(violations.isEmpty(), "非法入参必须被 Bean Validation 拦截");
        assertTrue(violations.size() >= 4, "预期拦截用户ID、商品ID、数量、地址长度、手机号及幂等键格式");

        // 2. 测试合法入参
        ToolOrderRequest validRequest = new ToolOrderRequest(
                "U001", "NB001", 1, new BigDecimal("8999.00"),
                "北京市海淀区中关村南大街1号院", "13800138000", "IDEM-TEST-12345678"
        );
        Set<ConstraintViolation<ToolOrderRequest>> validViolations = validator.validate(validRequest);
        assertTrue(validViolations.isEmpty(), "合法入参应零违规通过校验");
    }

    @Test
    @DisplayName("P18: 纯领域业务风控判定 - 超过 5000 元订单必须触发人在回路 (HITL) 审批")
    void testDomainRiskAssessment() {
        // 1. 低风险小额订单 (1件 2199 元耳机)
        ToolOrderRequest lowRiskReq = new ToolOrderRequest(
                "U001", "ACC001", 1, new BigDecimal("2199.00"),
                "北京市朝阳区酒仙桥路4号", "13912345678", "IDEM-LOW-001"
        );
        boolean lowRisk = domainService.isHighRiskAction("createOrder", lowRiskReq);
        assertFalse(lowRisk, "2199元订单处于免密风控阈值内");

        // 2. 高风险大额订单 (1件 8999 元拯救者，超过 5000 元阈值)
        ToolOrderRequest highRiskReq = new ToolOrderRequest(
                "U001", "NB001", 1, new BigDecimal("8999.00"),
                "北京市朝阳区酒仙桥路4号", "13912345678", "IDEM-HIGH-001"
        );
        boolean highRisk = domainService.isHighRiskAction("createOrder", highRiskReq);
        assertTrue(highRisk, "8999元订单必须触发高风险拦截与审批挂起");

        // 3. 选品打分与排序逻辑验证
        ShoppingIntent intent = new ShoppingIntent(
                "COMPARE", "笔记本电脑", new BigDecimal("10000.00"), List.of("联想"), "打游戏", true
        );
        List<Map<String, Object>> candidates = List.of(
                Map.of("id", "NB001", "name", "联想拯救者 Y9000P", "brand", "联想", "price", new BigDecimal("8999.00"), "stock", 18, "specs", "RTX4060"),
                Map.of("id", "NB002", "name", "苹果 MacBook Pro", "brand", "Apple", "price", new BigDecimal("13999.00"), "stock", 5, "specs", "M3 Pro")
        );
        Map<String, Double> ranked = domainService.scoreAndRankCandidates(candidates, intent);
        assertFalse(ranked.isEmpty());
        assertTrue(ranked.get("NB001") > ranked.get("NB002"), "联想拯救者因符合预算及品牌偏好应获得更高排序得分");
    }

    @Test
    @DisplayName("P07~P10: Checkpointer 状态全量快照、增量差分、断点复水与时光倒流分支测试")
    void testCheckpointStoreAndForking() {
        AgentScopeCheckpointStore store = new AgentScopeCheckpointStore();

        // 1. 保存初始快照 (Step 0)
        String execId = "exec-test-001";
        String cp0Id = "cp-step-0";
        AgentCheckpoint cp0 = new AgentCheckpoint(execId, cp0Id, null, WorkflowState.PLANNING, 0);
        cp0.setCallStack(List.of("Planning"));
        cp0.setScratchpad(Map.of("userIntent", "COMPARE_LAPTOPS"));
        store.saveSnapshot(cp0);

        // 2. 保存增量状态差分 (State Delta: Step 1 添加了候选集)
        store.saveDelta(execId, cp0Id, Map.of("candidatesFound", 3, "topCandidate", "NB001"));

        // 3. 验证复水读取 (Rehydration)
        AgentCheckpoint latest = store.loadLatestCheckpoint(execId);
        assertNotNull(latest, "必须成功复水已持久化的 Checkpoint");
        assertEquals(1, latest.getStepCursor());
        assertEquals("COMPARE_LAPTOPS", latest.getScratchpad().get("userIntent"), "继承了父节点的 scratchpad");
        assertEquals(3, latest.getScratchpad().get("candidatesFound"), "成功融合了 State Delta");

        // 4. 时光倒流分支 (Time-Travel Forking)
        String forkedExecId = "forked-exec-999";
        AgentCheckpoint forked = store.forkFromCheckpoint(execId, cp0Id, forkedExecId);
        assertNotNull(forked);
        assertEquals(forkedExecId, forked.getExecutionId());
        assertEquals(cp0Id, forked.getParentCheckpointId(), "父节点指向 Step 0");
        assertEquals("COMPARE_LAPTOPS", forked.getScratchpad().get("userIntent"));
        assertTrue(forked.getMetadata().containsKey("forkedFromExecutionId"));
    }

    @Test
    @DisplayName("P13~P14: AgentScope 28 类类型化 AgentEvent 转换为多通道 SSE 测试")
    void testAgentEventStreamBridge() {
        AgentEventStreamBridge bridge = new AgentEventStreamBridge(objectMapper);

        // 模拟 AgentScope 运行时产生的多通道类型化事件
        AgentEvent startEvent = new AgentStartEvent("session-1", "reply-1", "agent-1");
        AgentEvent thoughtDelta = new ThinkingBlockDeltaEvent("reply-1", "block-1", "正在对比两款笔记本的核心显卡算力...");
        AgentEvent toolCall = new ToolCallStartEvent("reply-1", "tc-101", "queryProductInfo");
        AgentEvent textDelta = new TextBlockDeltaEvent("reply-1", "block-2", "经过分析，拯救者Y9000P在游戏性能上表现更佳。");
        AgentEvent requireConfirm = new RequireUserConfirmEvent("cp-hitl-1", List.of(
                new ToolUseBlock("call-1", "createOrder", Map.of("productId", "NB001", "quantity", 1))
        ));

        Flux<AgentEvent> eventFlux = Flux.just(startEvent, thoughtDelta, toolCall, textDelta, requireConfirm);

        StepVerifier.create(bridge.bridgeToWorkflowStream("exec-stream-1", eventFlux))
                .assertNext(e -> {
                    assertEquals("step_start", e.getType());
                })
                .assertNext(e -> {
                    assertEquals("thought", e.getType());
                    assertEquals("正在对比两款笔记本的核心显卡算力...", e.getPayload());
                })
                .assertNext(e -> {
                    assertEquals("tool_start", e.getType());
                    assertTrue(e.getPayload().toString().contains("queryProductInfo"));
                })
                .assertNext(e -> {
                    assertEquals("token", e.getType());
                    assertTrue(e.getPayload().toString().contains("拯救者"));
                })
                .assertNext(e -> {
                    assertEquals("confirm_required", e.getType());
                })
                .verifyComplete();
    }
}
