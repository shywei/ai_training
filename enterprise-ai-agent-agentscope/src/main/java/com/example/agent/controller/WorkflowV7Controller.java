package com.example.agent.controller;

import com.example.agent.domain.AgentCheckpoint;
import com.example.agent.domain.WorkflowResponse;
import com.example.agent.domain.WorkflowStreamEvent;
import com.example.agent.port.CheckpointStorePort;
import com.example.agent.port.ShoppingAgentPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * REST & SSE 控制器：暴露 AgentScope 2.0 智能体工作流能力
 *
 * 对应第 09 课 P13~P14、P07~P10：
 * - /api/v7/workflow/execute       — 同步任务执行
 * - /api/v7/workflow/stream-events — 28 类类型化 AgentEvent 多通道流式输出 (SSE)
 * - /api/v7/workflow/confirm       — 人在回路 (HITL) 审批确认
 * - /api/v7/workflow/rehydrate     — 基于 Checkpoint 的跨进程断点复水
 * - /api/v7/workflow/fork          — 时光倒流分支演进 (Time-Travel Forking)
 * - /api/v7/workflow/checkpoints   — 查看快照历史血缘
 */
@RestController
@RequestMapping("/api/v7/workflow")
@CrossOrigin(origins = "*")
public class WorkflowV7Controller {

    private static final Logger log = LoggerFactory.getLogger(WorkflowV7Controller.class);

    private final ShoppingAgentPort shoppingAgentPort;
    private final CheckpointStorePort checkpointStore;

    public WorkflowV7Controller(ShoppingAgentPort shoppingAgentPort, CheckpointStorePort checkpointStore) {
        this.shoppingAgentPort = shoppingAgentPort;
        this.checkpointStore = checkpointStore;
    }

    /**
     * 1. 同步任务执行接口
     * POST /api/v7/workflow/execute
     */
    @PostMapping("/execute")
    public WorkflowResponse execute(
            @RequestParam String message,
            @RequestParam(required = false) String executionId,
            @RequestParam(defaultValue = "session-v7") String sessionId,
            @RequestParam(defaultValue = "user-v7") String userId) {

        String execId = executionId != null ? executionId : "v7-exec-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("[API-v7] 收到同步执行请求: execId={}, msg={}", execId, message);

        return shoppingAgentPort.executeTask(execId, sessionId, userId, message);
    }

    /**
     * 2. 28 类类型化 AgentEvent 多通道流式输出接口 (SSE)
     * GET /api/v7/workflow/stream-events
     * POST /api/v7/workflow/stream-events
     */
    @RequestMapping(value = "/stream-events", method = {RequestMethod.GET, RequestMethod.POST}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<WorkflowStreamEvent> streamEvents(
            @RequestParam String message,
            @RequestParam(required = false) String executionId,
            @RequestParam(defaultValue = "session-v7") String sessionId,
            @RequestParam(defaultValue = "user-v7") String userId) {

        String execId = executionId != null ? executionId : "v7-stream-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("[API-v7:SSE] 开启 AgentScope 28 类多通道事件流: execId={}, msg={}", execId, message);

        return shoppingAgentPort.streamTask(execId, sessionId, userId, message);
    }

    /**
     * 3. 人在回路 (HITL) 审批确认接口
     * POST /api/v7/workflow/confirm?executionId=xxx&checkpointId=xxx&confirmed=true
     */
    @PostMapping("/confirm")
    public WorkflowResponse confirm(
            @RequestParam String executionId,
            @RequestParam String checkpointId,
            @RequestParam boolean confirmed) {

        log.info("[API-v7:HITL] 收到审批反馈: execId={}, checkpointId={}, confirmed={}", executionId, checkpointId, confirmed);
        return shoppingAgentPort.confirmAction(executionId, checkpointId, confirmed);
    }

    /**
     * 4. 基于 Checkpoint 的跨进程断点复水 (Rehydration)
     * POST /api/v7/workflow/rehydrate?executionId=xxx&checkpointId=xxx
     */
    @PostMapping("/rehydrate")
    public WorkflowResponse rehydrate(
            @RequestParam String executionId,
            @RequestParam String checkpointId) {

        log.info("[API-v7:Rehydration] 发起断点复水恢复: execId={}, checkpointId={}", executionId, checkpointId);
        return shoppingAgentPort.rehydrateFromCheckpoint(executionId, checkpointId);
    }

    /**
     * 5. 时光倒流分支演进 (Time-Travel Forking)
     * POST /api/v7/workflow/fork?sourceExecutionId=xxx&checkpointId=xxx
     */
    @PostMapping("/fork")
    public Map<String, Object> fork(
            @RequestParam String sourceExecutionId,
            @RequestParam String checkpointId,
            @RequestParam(required = false) String newExecutionId) {

        String newExecId = (newExecutionId != null && !newExecutionId.isBlank())
                ? newExecutionId : "fork-" + UUID.randomUUID().toString().substring(0, 8);

        log.info("[API-v7:Fork] 触发时光倒流分支: source={}, cp={}, newExec={}", sourceExecutionId, checkpointId, newExecId);
        AgentCheckpoint forkedCheckpoint = checkpointStore.forkFromCheckpoint(sourceExecutionId, checkpointId, newExecId);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "FORKED_SUCCESS");
        resp.put("sourceExecutionId", sourceExecutionId);
        resp.put("baseCheckpointId", checkpointId);
        resp.put("newExecutionId", forkedCheckpoint.getExecutionId());
        resp.put("newCheckpointId", forkedCheckpoint.getCheckpointId());
        resp.put("stepCursor", forkedCheckpoint.getStepCursor());
        resp.put("callStack", forkedCheckpoint.getCallStack());
        return resp;
    }

    /**
     * 6. 查看快照版本血缘链条
     * GET /api/v7/workflow/checkpoints?executionId=xxx
     */
    @GetMapping("/checkpoints")
    public Map<String, Object> listCheckpoints(@RequestParam String executionId) {
        List<AgentCheckpoint> list = checkpointStore.getCheckpointHistory(executionId);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("executionId", executionId);
        resp.put("totalCheckpoints", list.size());
        resp.put("checkpoints", list.stream().map(cp -> Map.of(
                "checkpointId", cp.getCheckpointId(),
                "parentCheckpointId", Objects.toString(cp.getParentCheckpointId(), "ROOT"),
                "state", cp.getState().name(),
                "stepCursor", cp.getStepCursor(),
                "createdAt", cp.getCreatedAt().toString()
        )).toList());
        return resp;
    }
}
