package com.example.agent.adapter.springai2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Spring AI 2.0.2 GA 流式可观测性遥测拦截器 (TelemetryStreamAdvisor)
 *
 * 严格实现 Spring AI 2.0.2 GA 官方接口 org.springframework.ai.chat.client.advisor.api.StreamAdvisor
 * （替代早期 1.0-M4 的 StreamAroundAdvisor）：
 * 1. TTFT (Time to First Token)：首字生成延迟；
 * 2. 流式 Token 吞吐速率（Chunks/sec）；
 * 3. 与 Spring Boot 4.0 / Micrometer Observation 体系配合。
 */
@Component
public class TelemetryStreamAdvisor implements StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(TelemetryStreamAdvisor.class);

    public static final int ORDER = Ordered.LOWEST_PRECEDENCE - 100;

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        long startTime = System.currentTimeMillis();
        AtomicLong firstTokenTime = new AtomicLong(0);
        AtomicInteger chunkCounter = new AtomicInteger(0);
        AtomicBoolean isFirst = new AtomicBoolean(true);

        log.debug("[TelemetryAdvisor] 开始流式链路拦截, TraceId={}",
                request.context().getOrDefault("traceId", "UNKNOWN"));

        return chain.nextStream(request)
                .map(response -> {
                    long now = System.currentTimeMillis();
                    if (isFirst.compareAndSet(true, false)) {
                        long ttft = now - startTime;
                        firstTokenTime.set(ttft);
                        log.info("[TelemetryAdvisor] 🌟 首 Token 到达 (TTFT): {}ms", ttft);
                    }

                    int currentCount = chunkCounter.incrementAndGet();
                    return response.mutate()
                            .context("ttft_ms", firstTokenTime.get())
                            .context("stream_chunk_index", currentCount)
                            .build();
                })
                .doOnComplete(() -> {
                    long totalDuration = System.currentTimeMillis() - startTime;
                    int totalChunks = chunkCounter.get();
                    double cps = totalDuration > 0 ? (totalChunks * 1000.0 / totalDuration) : 0.0;
                    log.info("[TelemetryAdvisor] ✅ 流式响应结束: 总耗时={}ms, 分片数={}, 速率={}/s",
                            totalDuration, totalChunks, String.format("%.2f", cps));
                })
                .doOnError(err -> log.error("[TelemetryAdvisor] ❌ 流式链路发生异常: {}", err.getMessage()));
    }
}
