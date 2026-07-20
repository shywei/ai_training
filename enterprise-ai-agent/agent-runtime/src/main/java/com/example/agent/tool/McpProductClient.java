package com.example.agent.tool;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import jakarta.annotation.PreDestroy;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * MCP 商品查询客户端
 *
 * 连接外部 MCP Server（模拟企业 ERP），获取商品查询 Tool，
 * 并将每个 MCP Tool 包装为 Spring AI 的 FunctionCallback。
 *
 * 第八课核心教学点：
 * - MCP Client 是连接外部工具服务的标准协议
 * - SSE 传输：基于 HTTP 的 Server-Sent Events，适合远程服务
 * - Tool 发现：MCP Client 自动获取 Server 暴露的 Tool 列表
 * - 桥接：将 MCP Tool 转换为 Spring AI FunctionCallback，复用已有调用链路
 *
 * MCP SDK 1.1.3 同步 API：
 * - McpClient.sync(transport) → McpSyncClient
 * - client.initialize() → 协议握手
 * - client.listTools() → 获取 Tool 列表
 * - client.callTool(request) → 调用 Tool
 * - client.closeGracefully() → 优雅关闭
 */
@Component
public class McpProductClient {

    private final String serverUrl;
    private McpSyncClient mcpClient;
    private List<FunctionCallback> functionCallbacks = Collections.emptyList();
    private List<String> toolNames = Collections.emptyList();
    private boolean connected = false;

    public McpProductClient(@Value("${mcp.product.server-url:http://localhost:8081}") String serverUrl) {
        this.serverUrl = serverUrl;
    }

    /**
     * 启动时初始化 MCP Client 连接
     *
     * 使用 SSE 传输连接 MCP Server，获取 Tool 列表，
     * 并将每个 Tool 包装为 Spring AI FunctionCallback。
     *
     * 注意：如果 MCP Server 未启动，这里会捕获异常并打印日志，
     * 不会影响 Spring Boot 应用的正常启动。
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            System.out.println("[MCP] 正在连接 MCP Server: " + serverUrl);

            // SSE 传输层
            HttpClientSseClientTransport transport = HttpClientSseClientTransport
                    .builder(serverUrl)
                    .build();

            // 创建同步 Client
            mcpClient = McpClient.sync(transport)
                    .requestTimeout(Duration.ofSeconds(10))
                    .build();

            // 协议握手
            mcpClient.initialize();
            connected = true;
            System.out.println("[MCP] MCP Client 初始化成功");

            // 获取 Tool 列表
            ListToolsResult toolsResult = mcpClient.listTools();
            List<Tool> tools = toolsResult.tools();

            if (tools == null || tools.isEmpty()) {
                System.out.println("[MCP] MCP Server 未暴露任何 Tool");
                return;
            }

            System.out.println("[MCP] 发现 " + tools.size() + " 个 Tool:");

            List<FunctionCallback> callbacks = new ArrayList<>();
            List<String> names = new ArrayList<>();

            for (Tool tool : tools) {
                String toolName = tool.name();
                String toolDesc = tool.description();
                System.out.println("[MCP]   - " + toolName + ": " + toolDesc);

                // 将 MCP Tool 包装为 Spring AI FunctionCallback
                FunctionCallback callback = wrapMcpToolAsFunctionCallback(toolName, toolDesc);
                callbacks.add(callback);
                names.add(toolName);
            }

            this.functionCallbacks = Collections.unmodifiableList(callbacks);
            this.toolNames = Collections.unmodifiableList(names);

            System.out.println("[MCP] 已包装 " + callbacks.size() + " 个 FunctionCallback");

        } catch (Exception e) {
            System.err.println("[MCP] 连接 MCP Server 失败: " + e.getMessage());
            System.err.println("[MCP] MCP Tool 将不可用，请确保 MCP Server 已启动 (" + serverUrl + ")");
            connected = false;
        }
    }

    /**
     * 将单个 MCP Tool 包装为 Spring AI FunctionCallback
     *
     * MCP Tool 的 inputSchema 描述了参数结构，
     * Spring AI 的 FunctionCallbackWrapper 会从 Function 的泛型类型
     * 自动提取 JSON Schema。这里使用 Map<String, Object> 作为通用入参。
     */
    private FunctionCallback wrapMcpToolAsFunctionCallback(String toolName, String toolDescription) {
        // 使用 Map 作为通用输入，MCP Tool 的参数是 Map<String, Object>
        Function<Map<String, Object>, String> function = arguments -> {
            System.out.println("[MCP] 调用 MCP Tool: " + toolName + ", 参数: " + arguments);
            try {
                CallToolRequest request = new CallToolRequest(toolName, arguments);
                CallToolResult result = mcpClient.callTool(request);

                // 提取文本内容
                if (result.content() != null && !result.content().isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (Object content : result.content()) {
                        if (content instanceof TextContent textContent) {
                            sb.append(textContent.text());
                        } else {
                            sb.append(content.toString());
                        }
                    }
                    String response = sb.toString();
                    System.out.println("[MCP] Tool 响应: " + response);
                    return response;
                }
                return "MCP Tool " + toolName + " 返回空结果";
            } catch (Exception e) {
                System.err.println("[MCP] 调用 MCP Tool 失败: " + e.getMessage());
                return "调用 MCP Tool " + toolName + " 失败: " + e.getMessage();
            }
        };

        return FunctionCallbackWrapper.builder(function)
                .withName("mcp_" + toolName)
                .withDescription("[MCP] " + toolDescription)
                .build();
    }

    /**
     * 获取所有 MCP FunctionCallback
     *
     * 返回由 MCP Tool 包装而来的 FunctionCallback 列表，
     * 可直接注册到 ChatClient 中。
     */
    public List<FunctionCallback> getFunctionCallbacks() {
        return functionCallbacks;
    }

    /**
     * 获取所有 MCP Tool 名称（原始名称，不含 mcp_ 前缀）
     */
    public List<String> getToolNames() {
        return toolNames;
    }

    /**
     * 是否已成功连接 MCP Server
     */
    public boolean isConnected() {
        return connected && mcpClient != null;
    }

    /**
     * 优雅关闭 MCP Client 连接
     *
     * Spring 容器销毁时自动调用，释放 SSE 连接资源。
     */
    @PreDestroy
    public void close() {
        if (mcpClient != null) {
            try {
                System.out.println("[MCP] 正在关闭 MCP Client...");
                mcpClient.closeGracefully();
                System.out.println("[MCP] MCP Client 已关闭");
            } catch (Exception e) {
                System.err.println("[MCP] 关闭 MCP Client 异常: " + e.getMessage());
            }
        }
    }
}
