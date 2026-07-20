package com.example.agent.tool;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;

/**
 * MCP 知识检索客户端
 *
 * 连接 Knowledge MCP Server，提供四大能力：
 * 1. search_knowledge — 商品知识语义检索（RAG）
 * 2. search_memory   — 用户记忆语义检索
 * 3. save_memory     — 写入用户记忆
 * 4. user://{userId} — 读取用户画像（MCP Resource）
 *
 * 架构职责：
 * - Agent（agent-runtime）作为 MCP Client，不直接访问 pgvector
 * - Knowledge MCP Server 内部负责：Embedding、向量检索、结果格式化
 * - 记忆和知识统一通过 MCP 协议访问，解耦 Agent 与存储
 *
 * v5.0 变化：
 * - MemoryManager 不再直接调用 UserMemoryService（Spring Bean）
 * - 改为通过本 Client 调用 MCP Server 的 search_memory / save_memory
 */
@Component
public class McpKnowledgeClient {

    private static final Logger log = LoggerFactory.getLogger(McpKnowledgeClient.class);

    private final String serverUrl;
    private McpSyncClient mcpClient;
    private boolean connected = false;

    /** 仅 save_memory 包装为 FunctionCallback（给 LLM 主动调用） */
    private List<FunctionCallback> functionCallbacks = Collections.emptyList();
    private List<String> toolNames = Collections.emptyList();

    public McpKnowledgeClient(@Value("${mcp.knowledge.server-url:http://localhost:8082}") String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            System.out.println("[MCP-Knowledge] 正在连接 Knowledge MCP Server: " + serverUrl);

            HttpClientSseClientTransport transport = HttpClientSseClientTransport
                    .builder(serverUrl)
                    .build();

            mcpClient = McpClient.sync(transport)
                    .requestTimeout(Duration.ofSeconds(15))
                    .build();

            mcpClient.initialize();
            connected = true;
            System.out.println("[MCP-Knowledge] ✅ 连接成功，知识检索 + 记忆服务已就绪");

            // 发现 Tool 列表，将 save_memory 包装为 FunctionCallback
            discoverTools();

        } catch (Exception e) {
            System.err.println("[MCP-Knowledge] 连接失败: " + e.getMessage());
            System.err.println("[MCP-Knowledge] RAG 检索将不可用，请确保 Knowledge MCP Server 已启动 (" + serverUrl + ")");
            connected = false;
        }
    }

    /**
     * 调用知识库检索
     *
     * @param query 用户问题
     * @return 检索结果文本，未找到返回 null
     */
    public String search(String query) {
        if (!connected || mcpClient == null) {
            log.warn("[MCP-Knowledge] 未连接，跳过知识检索");
            return null;
        }

        try {
            log.info("[MCP-Knowledge] 知识检索: {}", query);

            CallToolResult result = mcpClient.callTool(
                    new CallToolRequest("search_knowledge",
                            Map.of("query", query))
            );

            return extractText(result);

        } catch (Exception e) {
            log.error("[MCP-Knowledge] 知识检索失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 语义检索用户记忆
     *
     * @param userId 用户 ID
     * @param query  查询文本
     * @return 格式化的记忆文本，未找到返回 null
     */
    public String searchMemory(String userId, String query) {
        if (!connected || mcpClient == null) {
            log.warn("[MCP-Knowledge] 未连接，跳过记忆检索");
            return null;
        }

        try {
            log.info("[MCP-Knowledge] 记忆检索: userId={}, query={}", userId, query);

            CallToolResult result = mcpClient.callTool(
                    new CallToolRequest("search_memory",
                            Map.of("user_id", userId, "query", query))
            );

            return extractText(result);

        } catch (Exception e) {
            log.error("[MCP-Knowledge] 记忆检索失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 保存用户记忆
     *
     * @param userId     用户 ID
     * @param memoryType 记忆类型：preference / conversation / event
     * @param content    记忆内容
     */
    public void saveMemory(String userId, String memoryType, String content) {
        if (!connected || mcpClient == null) {
            log.warn("[MCP-Knowledge] 未连接，跳过记忆保存");
            return;
        }

        try {
            log.info("[MCP-Knowledge] 保存记忆: userId={}, type={}", userId, memoryType);

            mcpClient.callTool(
                    new CallToolRequest("save_memory",
                            Map.of("user_id", userId,
                                    "memory_type", memoryType,
                                    "content", content))
            );

            log.info("[MCP-Knowledge] 记忆保存成功");

        } catch (Exception e) {
            log.error("[MCP-Knowledge] 记忆保存失败: {}", e.getMessage());
        }
    }

    /**
     * 从 MCP Tool 结果中提取文本
     */
    private String extractText(CallToolResult result) {
        if (result.content() != null && !result.content().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Object content : result.content()) {
                if (content instanceof TextContent textContent) {
                    sb.append(textContent.text());
                }
            }
            String response = sb.toString();
            return response.isBlank() ? null : response;
        }
        return null;
    }

    /**
     * 从 MCP Server 发现 Tool，将 save_memory 包装为 FunctionCallback
     *
     * 只包装 save_memory，因为：
     * - search_knowledge / search_memory 由系统内部调用（context 构建），不需要 LLM 调
     * - save_memory 需要 LLM 主动调用（用户表达偏好时保存记忆）
     */
    private void discoverTools() {
        try {
            ListToolsResult toolsResult = mcpClient.listTools();
            List<Tool> tools = toolsResult.tools();
            if (tools == null || tools.isEmpty()) return;

            List<FunctionCallback> callbacks = new ArrayList<>();
            List<String> names = new ArrayList<>();

            for (Tool tool : tools) {
                String name = tool.name();
                // 只把 save_memory 暴露给 LLM
                if ("save_memory".equals(name)) {
                    FunctionCallback cb = wrapMcpTool(tool.name(), tool.description());
                    callbacks.add(cb);
                    names.add(name);
                    log.info("[MCP-Knowledge] 暴露给 LLM 的 Tool: {}", name);
                }
            }

            this.functionCallbacks = Collections.unmodifiableList(callbacks);
            this.toolNames = Collections.unmodifiableList(names);

        } catch (Exception e) {
            log.error("[MCP-Knowledge] Tool 发现失败: {}", e.getMessage());
        }
    }

    /**
     * 将 MCP Tool 包装为 Spring AI FunctionCallback
     */
    private FunctionCallback wrapMcpTool(String toolName, String toolDescription) {
        Function<Map<String, Object>, String> function = arguments -> {
            log.info("[MCP-Knowledge] LLM 调用 Tool: {}, 参数: {}", toolName, arguments);
            try {
                CallToolResult result = mcpClient.callTool(new CallToolRequest(toolName, arguments));
                String response = extractText(result);
                log.info("[MCP-Knowledge] Tool 响应: {}", response);
                return response != null ? response : "操作完成";
            } catch (Exception e) {
                log.error("[MCP-Knowledge] Tool 调用失败: {}", e.getMessage());
                return "操作失败: " + e.getMessage();
            }
        };

        return FunctionCallbackWrapper.builder(function)
                .withName("mcp_" + toolName)
                .withDescription("[MCP] " + toolDescription)
                .build();
    }

    /**
     * 获取暴露给 LLM 的 FunctionCallback（当前仅 save_memory）
     */
    public List<FunctionCallback> getFunctionCallbacks() {
        return functionCallbacks;
    }

    /**
     * 获取暴露给 LLM 的 Tool 名称
     */
    public List<String> getToolNames() {
        return toolNames;
    }

    /**
     * 读取用户画像 Resource
     *
     * @param userId 用户 ID
     * @return JSON 格式的用户画像，未找到返回 null
     */
    public String readUserProfile(String userId) {
        if (!connected || mcpClient == null) {
            log.warn("[MCP-Knowledge] 未连接，跳过画像读取");
            return null;
        }

        try {
            log.info("[MCP-Knowledge] 读取用户画像: {}", userId);

            ReadResourceResult result = mcpClient.readResource(
                    new ReadResourceRequest("user://" + userId)
            );

            if (result.contents() != null && !result.contents().isEmpty()) {
                for (Object content : result.contents()) {
                    if (content instanceof TextResourceContents textContent) {
                        String profile = textContent.text();
                        log.info("[MCP-Knowledge] 画像读取成功");
                        return profile;
                    }
                }
            }
            return null;

        } catch (Exception e) {
            log.error("[MCP-Knowledge] 画像读取失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 索引商品知识库（通过 MCP 协议）
     *
     * @param force true 时清空后重新索引
     * @return 索引结果描述
     */
    public String indexKnowledge(boolean force) {
        if (!connected || mcpClient == null) {
            log.warn("[MCP-Knowledge] 未连接，跳过知识库索引");
            return "未连接 Knowledge MCP Server";
        }

        try {
            log.info("[MCP-Knowledge] 索引知识库, force={}", force);

            CallToolResult result = mcpClient.callTool(
                    new CallToolRequest("index_knowledge",
                            Map.of("force", force))
            );

            return extractText(result);

        } catch (Exception e) {
            log.error("[MCP-Knowledge] 知识库索引失败: {}", e.getMessage());
            return "索引失败: " + e.getMessage();
        }
    }

    public boolean isConnected() {
        return connected && mcpClient != null;
    }

    @PreDestroy
    public void close() {
        if (mcpClient != null) {
            try {
                mcpClient.closeGracefully();
                System.out.println("[MCP-Knowledge] Client 已关闭");
            } catch (Exception e) {
                System.err.println("[MCP-Knowledge] 关闭异常: " + e.getMessage());
            }
        }
    }
}
