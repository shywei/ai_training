package com.example.agent.workflow;

import com.example.agent.core.GuardrailsService;
import com.example.agent.memory.MemoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Workflow API 控制器
 *
 * 新增的工作流端点，不修改原有 ShoppingAgentController。
 * 复杂任务走 /api/agent/workflow，简单问题仍走 /api/agent/chat。
 *
 * 端点：
 * - POST /api/agent/workflow          — 复杂任务入口（走 AgentRuntime）
 * - POST /api/agent/workflow/confirm  — 用户确认/取消
 * - GET  /api/agent/workflow/status   — 查询执行状态
 * - GET  /api/agent/workflow/trace    — 查看执行链路
 * - GET  /api/agent/workflow/traces   — 查看所有 Trace
 */
@RestController
@RequestMapping("/api/agent/workflow")
public class WorkflowController {

    private static final Logger log = LoggerFactory.getLogger(WorkflowController.class);

    private final AgentRuntime agentRuntime;
    private final ConfirmationService confirmationService;
    private final TraceService traceService;
    private final ErrorRecovery errorRecovery;
    private final MemoryManager memoryManager;
    private final GuardrailsService guardrails;

    public WorkflowController(AgentRuntime agentRuntime,
                              ConfirmationService confirmationService,
                              TraceService traceService,
                              ErrorRecovery errorRecovery,
                              MemoryManager memoryManager,
                              GuardrailsService guardrails) {
        this.agentRuntime = agentRuntime;
        this.confirmationService = confirmationService;
        this.traceService = traceService;
        this.errorRecovery = errorRecovery;
        this.memoryManager = memoryManager;
        this.guardrails = guardrails;
    }

    /**
     * 统一任务入口
     *
     * POST /api/agent/workflow?message=xxx&sessionId=xxx&userId=xxx
     *
     * 前端统一调此端点，AgentRuntime 内部判断简单/复杂：
     * - 简单任务：TaskPlanner 跳过规划，一次 THINK 就回答
     * - 复杂任务：TaskPlanner 拆步骤，多轮 ReAct 循环
     *
     * /chat 端点保留，仅用于需要 SSE 流式输出的场景。
     */
    @PostMapping
    public Map<String, Object> execute(@RequestParam String message,
                                        @RequestParam(defaultValue = "default") String sessionId,
                                        @RequestParam(defaultValue = "") String userId) {
        log.info("[Workflow] 收到请求: {}", message);

        // 安全检查
        if (guardrails.checkInjection(message)) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status", "error");
            err.put("message", "检测到不安全的输入");
            return err;
        }

        // 创建 Trace
        TraceService.TraceRecord trace = traceService.createTrace(message);
        String traceId = trace.getTraceId();

        // 执行工作流
        AgentRuntime.WorkflowResult result = agentRuntime.execute(traceId, sessionId, userId, message);

        // 构建响应
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("traceId", traceId);

        // 判断执行模式：简单回答 vs 多步工作流
        int stepCount = trace != null ? trace.getSteps().size() : 0;
        response.put("mode", stepCount <= 1 ? "simple" : "workflow");
        response.put("stepCount", stepCount);

        if (result.isSuccess()) {
            response.put("status", "completed");
            response.put("message", result.getMessage());
            response.put("state", "COMPLETED");

            // 保存对话到记忆
            memoryManager.saveTurn(sessionId, userId, message, result.getMessage());
        } else if (result.isNeedsConfirmation()) {
            response.put("status", "confirming");
            response.put("message", result.getMessage());
            response.put("state", "CONFIRMING");
            response.put("confirmRequired", true);
            ConfirmationService.PendingConfirmation pending = result.getPendingConfirmation();
            if (pending != null) {
                response.put("pendingTool", pending.getToolName());
                response.put("pendingDescription", pending.getDescription());
            }
        } else {
            response.put("status", "failed");
            response.put("message", result.getMessage());
            response.put("state", "FAILED");
        }

        return response;
    }

    /**
     * 用户确认/取消
     *
     * POST /api/agent/workflow/confirm?traceId=xxx&confirmed=true&sessionId=xxx&userId=xxx&message=xxx
     */
    @PostMapping("/confirm")
    public Map<String, Object> confirm(@RequestParam String traceId,
                                        @RequestParam boolean confirmed,
                                        @RequestParam(defaultValue = "default") String sessionId,
                                        @RequestParam(defaultValue = "") String userId,
                                        @RequestParam(defaultValue = "") String message) {
        log.info("[Workflow] 确认请求: traceId={}, confirmed={}", traceId, confirmed);

        AgentRuntime.WorkflowResult result = agentRuntime.resumeAfterConfirm(
                traceId, confirmed, sessionId, userId, message);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("traceId", traceId);

        if (result.isSuccess()) {
            response.put("status", "completed");
            response.put("message", result.getMessage());
            response.put("state", "COMPLETED");

            // 保存对话到记忆
            if (message != null && !message.isBlank()) {
                memoryManager.saveTurn(sessionId, userId, message, result.getMessage());
            }
        } else {
            response.put("status", "failed");
            response.put("message", result.getMessage());
            response.put("state", "FAILED");
        }

        return response;
    }

    /**
     * 查询执行状态
     *
     * GET /api/agent/workflow/status?traceId=xxx
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus(@RequestParam String traceId) {
        TraceService.TraceRecord trace = traceService.getTrace(traceId);

        Map<String, Object> response = new LinkedHashMap<>();
        if (trace == null) {
            response.put("status", "not_found");
            response.put("message", "Trace 不存在: " + traceId);
            return response;
        }

        response.put("traceId", traceId);
        response.put("state", trace.getState().name());
        response.put("userMessage", trace.getUserMessage());
        response.put("stepCount", trace.getSteps().size());
        response.put("totalDurationMs", trace.getTotalDurationMs());
        response.put("summary", trace.getSummary());

        // 步骤详情
        response.put("steps", trace.getSteps().stream().map(step -> {
            Map<String, Object> stepMap = new LinkedHashMap<>();
            stepMap.put("step", step.getStepNumber());
            stepMap.put("action", step.getAction());
            stepMap.put("status", step.getStatus().name());
            stepMap.put("toolName", step.getToolName());
            stepMap.put("durationMs", step.getDurationMs());
            stepMap.put("retryCount", step.getRetryCount());
            if (step.getErrorMessage() != null) {
                stepMap.put("error", step.getErrorMessage());
            }
            return stepMap;
        }).toList());

        return response;
    }

    /**
     * 查看执行链路
     *
     * GET /api/agent/workflow/trace?traceId=xxx
     */
    @GetMapping("/trace")
    public Map<String, Object> getTrace(@RequestParam String traceId) {
        TraceService.TraceRecord trace = traceService.getTrace(traceId);

        Map<String, Object> response = new LinkedHashMap<>();
        if (trace == null) {
            response.put("status", "not_found");
            return response;
        }

        response.put("traceId", traceId);
        response.put("state", trace.getState().name());
        response.put("userMessage", trace.getUserMessage());
        response.put("startTime", trace.getStartTime().toString());
        response.put("endTime", trace.getEndTime() != null ? trace.getEndTime().toString() : null);
        response.put("totalDurationMs", trace.getTotalDurationMs());
        response.put("summary", trace.getSummary());
        response.put("events", trace.getEvents());

        // 步骤详情（含思考过程）
        response.put("steps", trace.getSteps().stream().map(step -> {
            Map<String, Object> stepMap = new LinkedHashMap<>();
            stepMap.put("step", step.getStepNumber());
            stepMap.put("action", step.getAction());
            stepMap.put("status", step.getStatus().name());
            stepMap.put("toolName", step.getToolName());
            stepMap.put("toolInput", step.getToolInput());
            stepMap.put("toolOutput", step.getToolOutput());
            stepMap.put("llmThought", step.getLlmThought());
            stepMap.put("durationMs", step.getDurationMs());
            stepMap.put("retryCount", step.getRetryCount());
            return stepMap;
        }).toList());

        return response;
    }

    /**
     * 查看所有 Trace
     *
     * GET /api/agent/workflow/traces
     */
    @GetMapping("/traces")
    public Map<String, Object> getTraces() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("traces", traceService.getRecentTraces().stream().map(trace -> {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("traceId", trace.getTraceId());
            t.put("userMessage", trace.getUserMessage());
            t.put("state", trace.getState().name());
            t.put("totalDurationMs", trace.getTotalDurationMs());
            t.put("stepCount", trace.getSteps().size());
            return t;
        }).toList());
        return response;
    }

    /**
     * 查看熔断状态
     *
     * GET /api/agent/workflow/circuit-breaker
     */
    @GetMapping("/circuit-breaker")
    public Map<String, Object> getCircuitBreakerStatus() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", errorRecovery.getStatus());
        return response;
    }

    /**
     * 重置熔断器
     *
     * POST /api/agent/workflow/circuit-breaker/reset
     */
    @PostMapping("/circuit-breaker/reset")
    public Map<String, Object> resetCircuitBreaker() {
        errorRecovery.resetAll();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "ok");
        response.put("message", "所有熔断计数器已重置");
        return response;
    }

    /**
     * 流式任务入口（SSE）
     *
     * POST /api/agent/workflow/stream?message=xxx&sessionId=xxx&userId=xxx
     *
     * 执行逻辑与 /workflow 完全一致，但返回 SSE 流：
     * 1. [meta] 事件：traceId, mode, stepCount
     * 2. [token] 事件：最终回答逐字流式输出
     * 3. [done] 事件：完成标记
     *
     * 前端可用 EventSource 或 fetch+ReadableStream 接收。
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> executeStream(@RequestParam String message,
                                       @RequestParam(defaultValue = "default") String sessionId,
                                       @RequestParam(defaultValue = "") String userId) {
        log.info("[Workflow-Stream] 收到请求: {}", message);

        // 安全检查（事件统一用 type 字段，前端按 type 分发）
        // ⚠️ 返回裸 JSON 即可：Spring MVC 对 Flux<String> + text/event-stream
        //    会自动包一层 SSE 框架（data: 前缀），手工再拼会导致 "data:data:{...}"
        //    前端 JSON.parse 全部静默失败 → 显示"（无响应）"
        if (guardrails.checkInjection(message)) {
            return Flux.just(
                    "{\"type\":\"error\",\"message\":\"检测到不安全的输入\"}",
                    "{\"type\":\"done\",\"status\":\"completed\"}");
        }

        // 创建 Trace
        TraceService.TraceRecord trace = traceService.createTrace(message);
        String traceId = trace.getTraceId();

        // 立即发送 meta 事件，避免前端超时
        String metaEvent = String.format(
                "{\"type\":\"meta\",\"traceId\":\"%s\",\"mode\":\"processing\"}", traceId);

        // 用 Flux.defer 包装同步执行，先发 meta 再执行工作流。
        //
        // ⚠️ 工作流是同步执行的（THINK→ACT→OBSERVE 多轮 LLM 调用），
        //    第一个 token 之前可能有较长的静默期。
        //    两个保护措施：
        //    1. 执行期间每 10 秒发一个 ping 事件保活（防中间层闲置断连）
        //    2. application.yml 配置 spring.mvc.async.request-timeout=600000
        //       （默认约 30 秒，长任务会在第一个 token 前被容器掐断 → 前端显示"（无响应）"）
        Flux<String> workFlux = Flux.defer(() -> {
                    // 执行工作流（同步）
                    AgentRuntime.WorkflowResult result = agentRuntime.execute(traceId, sessionId, userId, message);
                    int stepCount = trace.getSteps().size();
                    String mode = stepCount <= 1 ? "simple" : "workflow";

                    if (result.isSuccess()) {
                        try {
                            memoryManager.saveTurn(sessionId, userId, message, result.getMessage());
                        } catch (Exception e) {
                            // 记忆保存失败不能影响回复输出
                            log.warn("[Workflow-Stream] saveTurn 失败（不影响回复）: {}", e.getMessage());
                        }
                        String answer = guardrails.sanitize(result.getMessage());
                        String finalAnswer = answer != null ? answer : "";

                        if (finalAnswer.isEmpty()) {
                            return Flux.just("{\"type\":\"token\",\"content\":\"（无响应）\"}",
                                    "{\"type\":\"done\",\"status\":\"completed\"}");
                        }

                        // 逐 chunk 流式输出（每 20 字符一组，减少事件数）
                        int chunkSize = 20;
                        List<String> chunks = new java.util.ArrayList<>();
                        for (int i = 0; i < finalAnswer.length(); i += chunkSize) {
                            chunks.add(finalAnswer.substring(i, Math.min(i + chunkSize, finalAnswer.length())));
                        }

                        Flux<String> tokenFlux = Flux.fromIterable(chunks)
                                .delayElements(Duration.ofMillis(80))
                                .map(chunk -> "{\"type\":\"token\",\"content\":\"" + escapeJson(chunk) + "\"}");

                        String doneEvent = "{\"type\":\"done\",\"status\":\"completed\"}";
                        return Flux.concat(tokenFlux, Flux.just(doneEvent));

                    } else if (result.isNeedsConfirmation()) {
                        String confirmEvent = String.format(
                                "{\"type\":\"confirm\",\"message\":\"%s\",\"traceId\":\"%s\"}",
                                escapeJson(result.getMessage()), traceId);
                        return Flux.just(confirmEvent);

                    } else {
                        String failEvent = String.format(
                                "{\"type\":\"error\",\"message\":\"%s\"}",
                                escapeJson(result.getMessage()));
                        return Flux.just(failEvent);
                    }
                })
                // 工作流在独立线程执行，避免阻塞订阅线程
                .subscribeOn(Schedulers.boundedElastic())
                // 单次执行，供 ping 停止信号复用
                .cache();

        // 保活 ping：工作流静默执行期间每 10 秒发一次，首包输出后自动停止
        Flux<String> pingFlux = Flux.interval(Duration.ofSeconds(10))
                .map(i -> "{\"type\":\"ping\"}")
                .takeUntilOther(workFlux);

        return Flux.concat(
                Flux.just(metaEvent),
                Flux.merge(workFlux, pingFlux)
        ).onErrorResume(e -> {
            // 任何异常都必须以 error 事件收尾，不能让前端拿到静默 EOF（会显示"（无响应）"）
            log.error("[Workflow-Stream] 执行异常", e);
            return Flux.just(
                    "{\"type\":\"error\",\"message\":\"服务内部错误，请查看后端日志\"}",
                    "{\"type\":\"done\",\"status\":\"error\"}");
        });
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}