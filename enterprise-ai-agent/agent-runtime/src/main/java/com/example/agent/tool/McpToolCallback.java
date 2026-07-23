package com.example.agent.tool;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.function.FunctionCallback;

import java.util.Map;

/**
 * MCP Tool → Spring AI FunctionCallback 桥接器
 *
 * 解决 FunctionCallbackWrapper 无法从 Map<String, Object> 推断 JSON Schema 的问题。
 * 直接使用 MCP Tool 的 inputSchema 作为函数参数定义。
 */
public class McpToolCallback implements FunctionCallback {

    private static final Logger log = LoggerFactory.getLogger(McpToolCallback.class);

    private final String name;
    private final String description;
    private final String inputSchema;
    private final McpSyncClient mcpClient;
    private final String originalToolName;

    public McpToolCallback(String name, String description, String inputSchema,
                           McpSyncClient mcpClient, String originalToolName) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
        this.mcpClient = mcpClient;
        this.originalToolName = originalToolName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getInputTypeSchema() {
        return inputSchema;
    }

    @Override
    public String call(String functionInput) {
        log.info("[MCP] 调用 Tool: {}, 参数: {}", originalToolName, functionInput);
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = mapper.readValue(functionInput, Map.class);

            CallToolResult result = mcpClient.callTool(new CallToolRequest(originalToolName, arguments));

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
                log.info("[MCP] Tool 响应: {}", response);
                return response;
            }
            return "MCP Tool " + originalToolName + " 返回空结果";
        } catch (Exception e) {
            log.error("[MCP] 调用 Tool 失败: {}", e.getMessage(), e);
            return "调用 MCP Tool " + originalToolName + " 失败: " + e.getMessage();
        }
    }
}
