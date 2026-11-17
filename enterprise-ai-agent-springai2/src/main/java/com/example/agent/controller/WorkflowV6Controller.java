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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring AI 2.0 现代基础设施对外控制器 (WorkflowV6Controller)
 *
 * 对应第 09 课 P03、P07~P10 与 P20~P24：
 * 1. 兼容原手搓 v5.0 前端调用的 REST + SSE 基础协议；
 * 2. 新增深度深水区特性接口：
 *    - /api/v6/workflow/rehydrate（断点复水：任务跨 Pod 漂移或重启后无损恢复）
 *    - /api/v6/workflow/fork（时光倒流：从任意历史 Checkpoint 分叉独立决策树）
 *    - /api/v6/workflow/checkpoints（快照因果溯源链查询）
 */
@RestController
@RequestMapping("/api/v6/workflow")
@CrossOrigin(origins = "*")
public class WorkflowV6Controller {

    private static final Logger log = LoggerFactory.getLogger(WorkflowV6Controller.class);

    private final ShoppingAgentPort agentPort;
    private final CheckpointStorePort checkpointStore;

    public WorkflowV6Controller(ShoppingAgentPort agentPort,
                                CheckpointStorePort checkpointStore) {
        this.agentPort = agentPort;
        this.checkpointStore = checkpointStore;
    }

    /**
     * 任务执行入口（同步等待完整结果）
     *
     * POST /api/v6/workflow?message=xxx&sessionId=xxx&userId=xxx
     */
    @PostMapping
    public WorkflowResponse execute(@RequestParam String message,
                                    @RequestParam(defaultValue = "default") String sessionId,
                                    @RequestParam(defaultValue = "U001") String userId) {
        String traceId = "trace-v6-" + System.currentTimeMillis();
        log.info("[API v6] 收到任务执行请求: message={}, userId={}", message, userId);
        return agentPort.executeTask(traceId, sessionId, userId, message);
    }

    /**
     * SSE 流式执行接口（支持前端逐字与步骤事件实时渲染）
     *
     * GET /api/v6/workflow/stream?message=xxx&sessionId=xxx&userId=xxx
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<WorkflowStreamEvent> streamExecute(@RequestParam String message,
                                                   @RequestParam(defaultValue = "default") String sessionId,
                                                   @RequestParam(defaultValue = "U001") String userId) {
        String traceId = "trace-v6-stream-" + System.currentTimeMillis();
        log.info("[API v6] 收到流式任务执行请求: message={}", message);
        return agentPort.streamTask(traceId, sessionId, userId, message);
    }

    /**
     * 人在回路（HITL）人工确认/取消接口
     *
     * POST /api/v6/workflow/confirm?executionId=xxx&checkpointId=xxx&confirmed=true
     */
    @PostMapping("/confirm")
    public WorkflowResponse confirm(@RequestParam String executionId,
                                    @RequestParam String checkpointId,
                                    @RequestParam boolean confirmed) {
        log.info("[API v6] 收到人在回路确认: executionId={}, checkpointId={}, confirmed={}",
                executionId, checkpointId, confirmed);
        return agentPort.confirmAction(executionId, checkpointId, confirmed);
    }

    /**
     * 【深度深水区 1】断点复水接口（Rehydration）
     *
     * POST /api/v6/workflow/rehydrate?executionId=xxx&checkpointId=xxx
     *
     * 解决场景：长任务中断、服务发布重启、容器 Pod 漂移后的零丢失状态恢复。
     */
    @PostMapping("/rehydrate")
    public WorkflowResponse rehydrate(@RequestParam String executionId,
                                      @RequestParam String checkpointId) {
        log.info("[API v6] 收到任务复水请求: executionId={}, checkpointId={}", executionId, checkpointId);
        return agentPort.rehydrateFromCheckpoint(executionId, checkpointId);
    }

    /**
     * 【深度深水区 1】时光倒流与分支衍生接口（Time-Travel / Forking）
     *
     * POST /api/v6/workflow/fork?sourceExecutionId=xxx&checkpointId=xxx&newExecutionId=xxx
     *
     * 解决场景：某一步决策不理想或失败，回滚到指定历史 Checkpoint，探索不同的分支路径。
     */
    @PostMapping("/fork")
    public Map<String, Object> forkBranch(@RequestParam String sourceExecutionId,
                                          @RequestParam String checkpointId,
                                          @RequestParam(required = false) String newExecutionId) {
        if (newExecutionId == null || newExecutionId.isBlank()) {
            newExecutionId = "exec-forked-" + System.currentTimeMillis();
        }

        log.info("[API v6] 收到分支回滚时光倒流请求: 原任务 [{}::{}], 新分支 [{}]",
                sourceExecutionId, checkpointId, newExecutionId);

        AgentCheckpoint forked = checkpointStore.forkFromCheckpoint(sourceExecutionId, checkpointId, newExecutionId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "FORKED_SUCCESS");
        result.put("sourceExecutionId", sourceExecutionId);
        result.put("targetCheckpointId", checkpointId);
        result.put("newExecutionId", newExecutionId);
        result.put("newCheckpointId", forked.getCheckpointId());
        result.put("state", forked.getState());
        result.put("message", "已成功从历史快照点回滚并分叉新分支，可调用 /rehydrate 接口继续推进该分支。");
        return result;
    }

    /**
     * 查询指定执行链路的快照因果 DAG 历史
     *
     * GET /api/v6/workflow/checkpoints?executionId=xxx
     */
    @GetMapping("/checkpoints")
    public List<AgentCheckpoint> getCheckpoints(@RequestParam String executionId) {
        return checkpointStore.getCheckpointHistory(executionId);
    }

    /**
     * 查询任务当前最新状态
     *
     * GET /api/v6/workflow/status?executionId=xxx
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus(@RequestParam String executionId) {
        AgentCheckpoint latest = checkpointStore.loadLatestCheckpoint(executionId);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("executionId", executionId);
        if (latest != null) {
            res.put("checkpointId", latest.getCheckpointId());
            res.put("state", latest.getState());
            res.put("stepCursor", latest.getStepCursor());
            res.put("pendingConfirmation", latest.getPendingConfirmation());
            res.put("createdAt", latest.getCreatedAt());
        } else {
            res.put("state", "NOT_FOUND");
        }
        return res;
    }
}
