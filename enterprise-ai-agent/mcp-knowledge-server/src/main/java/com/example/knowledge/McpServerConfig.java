package com.example.knowledge;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * MCP Server 配置类。
 *
 * 注册 4 个 Tool + 1 个 Resource：
 * - search_knowledge：商品知识语义检索（RAG）
 * - search_memory：用户记忆语义检索
 * - save_memory：写入用户记忆
 * - index_knowledge：索引商品知识库
 * - user://{userId}：用户画像（Resource）
 */
@Configuration
public class McpServerConfig {

    @Bean
    public HttpServletSseServerTransportProvider transportProvider() {
        return HttpServletSseServerTransportProvider.builder()
                .messageEndpoint("/mcp/message")
                .build();
    }

    @Bean
    public ServletRegistrationBean<?> servletBean(HttpServletSseServerTransportProvider tp) {
        return new ServletRegistrationBean<>(tp, "/*");
    }

    @Bean
    public McpSyncServer mcpSyncServer(HttpServletSseServerTransportProvider tp,
                                        VectorSearchService searchService,
                                        MemoryService memoryService,
                                        UserProfileService userProfileService,
                                        KnowledgeIndexer knowledgeIndexer) {

        // ===== Tool 1: search_knowledge =====
        McpSchema.Tool searchKnowledgeTool = McpSchema.Tool.builder()
                .name("search_knowledge")
                .description("从商品知识库中检索相关信息。当用户询问商品推荐、商品配置、商品对比时调用。")
                .inputSchema(jsonSchema("""
                        {
                            "type": "object",
                            "properties": {
                                "query": {
                                    "type": "string",
                                    "description": "用户的问题或搜索关键词"
                                }
                            },
                            "required": ["query"]
                        }
                        """))
                .build();

        McpServerFeatures.SyncToolSpecification searchKnowledgeSpec = new McpServerFeatures.SyncToolSpecification(
                searchKnowledgeTool,
                (exchange, request) -> {
                    String query = (String) request.arguments().get("query");
                    String result = searchService.search(query);
                    return McpSchema.CallToolResult.builder()
                            .addTextContent(result != null ? result : "未找到相关商品信息")
                            .isError(result == null)
                            .build();
                }
        );

        // ===== Tool 2: search_memory =====
        McpSchema.Tool searchMemoryTool = McpSchema.Tool.builder()
                .name("search_memory")
                .description("检索用户的历史记忆和偏好。当需要了解用户之前的偏好、需求、对话历史时调用。")
                .inputSchema(jsonSchema("""
                        {
                            "type": "object",
                            "properties": {
                                "user_id": {
                                    "type": "string",
                                    "description": "用户 ID"
                                },
                                "query": {
                                    "type": "string",
                                    "description": "搜索关键词，用于语义匹配用户记忆"
                                }
                            },
                            "required": ["user_id", "query"]
                        }
                        """))
                .build();

        McpServerFeatures.SyncToolSpecification searchMemorySpec = new McpServerFeatures.SyncToolSpecification(
                searchMemoryTool,
                (exchange, request) -> {
                    String userId = (String) request.arguments().get("user_id");
                    String query = (String) request.arguments().get("query");
                    String result = memoryService.search(userId, query);
                    return McpSchema.CallToolResult.builder()
                            .addTextContent(result != null ? result : "未找到相关记忆")
                            .isError(result == null)
                            .build();
                }
        );

        // ===== Tool 3: save_memory =====
        McpSchema.Tool saveMemoryTool = McpSchema.Tool.builder()
                .name("save_memory")
                .description("记录对话中发生的事实。当对话中有重要信息（用户做了决定、提到了关键需求）时调用。记录事实，不要提炼偏好。")
                .inputSchema(jsonSchema("""
                        {
                            "type": "object",
                            "properties": {
                                "user_id": {
                                    "type": "string",
                                    "description": "用户 ID"
                                },
                                "memory_type": {
                                    "type": "string",
                                    "description": "记忆类型：conversation（对话摘要）/ event（重要事件，如用户做了决定）",
                                    "enum": ["conversation", "event"]
                                },
                                "content": {
                                    "type": "string",
                                    "description": "记忆内容，用自然语言描述"
                                }
                            },
                            "required": ["user_id", "memory_type", "content"]
                        }
                        """))
                .build();

        McpServerFeatures.SyncToolSpecification saveMemorySpec = new McpServerFeatures.SyncToolSpecification(
                saveMemoryTool,
                (exchange, request) -> {
                    String userId = (String) request.arguments().get("user_id");
                    String memoryType = (String) request.arguments().get("memory_type");
                    String content = (String) request.arguments().get("content");
                    memoryService.save(userId, memoryType, content);
                    return McpSchema.CallToolResult.builder()
                            .addTextContent("记忆已保存：" + content)
                            .isError(false)
                            .build();
                }
        );

        // ===== Tool 4: index_knowledge =====
        McpSchema.Tool indexKnowledgeTool = McpSchema.Tool.builder()
                .name("index_knowledge")
                .description("索引商品知识库。首次使用或需要更新知识库时调用。force=true 会清空旧数据重新索引。")
                .inputSchema(jsonSchema("""
                        {
                            "type": "object",
                            "properties": {
                                "force": {
                                    "type": "boolean",
                                    "description": "是否强制重新索引（清空旧数据后重建）"
                                }
                            },
                            "required": ["force"]
                        }
                        """))
                .build();

        McpServerFeatures.SyncToolSpecification indexKnowledgeSpec = new McpServerFeatures.SyncToolSpecification(
                indexKnowledgeTool,
                (exchange, request) -> {
                    Boolean force = (Boolean) request.arguments().get("force");
                    String result = knowledgeIndexer.index(Boolean.TRUE.equals(force));
                    return McpSchema.CallToolResult.builder()
                            .addTextContent(result)
                            .isError(false)
                            .build();
                }
        );

        // ===== Resource Template: user://{userId} =====
        McpSchema.ResourceTemplate userProfileTemplate = McpSchema.ResourceTemplate.builder()
                .uriTemplate("user://{userId}")
                .name("用户画像")
                .description("获取指定用户的画像信息（事实、偏好）")
                .mimeType("application/json")
                .build();

        McpServerFeatures.SyncResourceTemplateSpecification userResourceSpec = new McpServerFeatures.SyncResourceTemplateSpecification(
                userProfileTemplate,
                (exchange, request) -> {
                    String uri = request.uri();
                    String userId = uri.substring("user://".length());
                    String profile = userProfileService.getProfile(userId);

                    if (profile == null) {
                        profile = "{\"user_id\": \"" + userId + "\", \"facts\": \"\", \"preferences\": \"\"}";
                    }

                    return new McpSchema.ReadResourceResult(
                            List.of(new McpSchema.TextResourceContents(uri, "application/json", profile))
                    );
                }
        );

        // ===== 组装 MCP Server =====
        return McpServer.sync(tp)
                .serverInfo("knowledge-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .resources(true, true)
                        .build())
                .tools(searchKnowledgeSpec, searchMemorySpec, saveMemorySpec, indexKnowledgeSpec)
                .resourceTemplates(userResourceSpec)
                .build();
    }

    /**
     * JSON Schema 字符串转 McpSchema.JsonSchema record
     */
    private McpSchema.JsonSchema jsonSchema(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> map = mapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            String type = (String) map.getOrDefault("type", "object");
            java.util.Map<String, Object> properties = (java.util.Map<String, Object>) map.getOrDefault("properties", java.util.Map.of());
            java.util.List<String> required = (java.util.List<String>) map.getOrDefault("required", java.util.List.of());
            return new McpSchema.JsonSchema(type, properties, required, null, null, null);
        } catch (Exception e) {
            throw new RuntimeException("JSON Schema 解析失败", e);
        }
    }
}
