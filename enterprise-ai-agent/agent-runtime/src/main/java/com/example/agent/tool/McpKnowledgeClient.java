package com.example.agent.tool;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * MCP 知识检索客户端
 *
 * 连接 Knowledge MCP Server，提供四大能力：
 * 1. search_knowledge — 商品知识语义检索（RAG）
 * 2. search_memory   — 用户记忆语义检索
 * 3. save_memory     — 写入用户记忆（系统自动沉淀，不暴露给 LLM）
 * 4. user://{userId} — 读取用户画像（MCP Resource）
 *
 * 记忆保存由系统控制（MemoryManager 自动沉淀 + 清空会话），
 * 不通过 LLM FunctionCallback 调用。
 */
@Component
public class McpKnowledgeClient {

    private static final Logger log = LoggerFactory.getLogger(McpKnowledgeClient.class);

    private final String serverUrl;
    private McpSyncClient mcpClient;
    private boolean connected = false;

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

        } catch (Exception e) {
            System.err.println("[MCP-Knowledge] 连接失败: " + e.getMessage());
            System.err.println("[MCP-Knowledge] 请确保 Knowledge MCP Server 已启动 (" + serverUrl + ")");
            connected = false;
        }
    }

    /**
     * 商品知识语义检索（RAG）
     */
    public String search(String query) {
        if (!connected || mcpClient == null) {
            log.warn("[MCP-Knowledge] 未连接，跳过知识检索");
            return null;
        }
        try {
            log.info("[MCP-Knowledge] 知识检索: {}", query);
            CallToolResult result = mcpClient.callTool(
                    new CallToolRequest("search_knowledge", Map.of("query", query)));
            return extractText(result);
        } catch (Exception e) {
            log.error("[MCP-Knowledge] 知识检索失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 用户记忆语义检索
     */
    public String searchMemory(String userId, String query) {
        if (!connected || mcpClient == null) {
            log.warn("[MCP-Knowledge] 未连接，跳过记忆检索");
            return null;
        }
        try {
            log.info("[MCP-Knowledge] 记忆检索: userId={}, query={}", userId, query);
            CallToolResult result = mcpClient.callTool(
                    new CallToolRequest("search_memory", Map.of("user_id", userId, "query", query)));
            return extractText(result);
        } catch (Exception e) {
            log.error("[MCP-Knowledge] 记忆检索失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 保存用户记忆（系统调用，不暴露给 LLM）
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
                            Map.of("user_id", userId, "memory_type", memoryType, "content", content)));
            log.info("[MCP-Knowledge] 记忆保存成功");
        } catch (Exception e) {
            log.error("[MCP-Knowledge] 记忆保存失败: {}", e.getMessage());
        }
    }

    /**
     * 读取用户画像（MCP Resource）
     */
    public String readUserProfile(String userId) {
        if (!connected || mcpClient == null) {
            log.warn("[MCP-Knowledge] 未连接，跳过画像读取");
            return null;
        }
        try {
            log.info("[MCP-Knowledge] 读取用户画像: {}", userId);
            ReadResourceResult result = mcpClient.readResource(
                    new ReadResourceRequest("user://" + userId));
            if (result.contents() != null && !result.contents().isEmpty()) {
                for (Object content : result.contents()) {
                    if (content instanceof TextResourceContents textContent) {
                        log.info("[MCP-Knowledge] 画像读取成功");
                        return textContent.text();
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
     * 索引商品知识库
     */
    public String indexKnowledge(boolean force) {
        if (!connected || mcpClient == null) {
            log.warn("[MCP-Knowledge] 未连接，跳过知识库索引");
            return "未连接 Knowledge MCP Server";
        }
        try {
            log.info("[MCP-Knowledge] 索引知识库, force={}", force);
            CallToolResult result = mcpClient.callTool(
                    new CallToolRequest("index_knowledge", Map.of("force", force)));
            return extractText(result);
        } catch (Exception e) {
            log.error("[MCP-Knowledge] 知识库索引失败: {}", e.getMessage());
            return "索引失败: " + e.getMessage();
        }
    }

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
