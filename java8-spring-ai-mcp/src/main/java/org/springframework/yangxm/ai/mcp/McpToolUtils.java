package org.springframework.yangxm.ai.mcp;

import io.modelcontextprotocol.yangxm.ai.mcp.json.McpJsonMapper;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.Annotations;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.CallToolRequest;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.CallToolResult;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.Content;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.ImageContent;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.Role;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.Tool;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.SyncToolSpec;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpSyncServerExchange;
import io.modelcontextprotocol.yangxm.ai.mcp.util.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import org.springframework.yangxm.ai.model.chat.model.ToolContext;
import org.springframework.yangxm.ai.model.tool.ToolCallback;
import org.springframework.yangxm.ai.util.Maps;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public final class McpToolUtils {
    public static final String TOOL_CONTEXT_MCP_EXCHANGE_KEY = "exchange";
    private static final McpJsonMapper JSON_MAPPER = McpJsonMapper.createDefault();

    private McpToolUtils() {
    }

    public static String prefixedToolName(String prefix, @Nullable String title, String toolName) {
        if (StringUtils.isEmpty(prefix) || StringUtils.isEmpty(toolName)) {
            throw new IllegalArgumentException("Prefix or toolName cannot be null or empty");
        }

        String input = shorten(format(prefix));
        if (!StringUtils.isEmpty(title)) {
            input = input + "_" + format(title);
        }

        input = input + "_" + format(toolName);

        if (input.length() > 64) {
            input = input.substring(input.length() - 64);
        }

        return input;
    }

    public static String prefixedToolName(String prefix, String toolName) {
        return prefixedToolName(prefix, null, toolName);
    }

    private static String format(String input) {
        String formatted = input.replaceAll("[^\\p{IsHan}\\p{InCJK_Unified_Ideographs}\\p{InCJK_Compatibility_Ideographs}a-zA-Z0-9_-]", "");
        return formatted.replaceAll("-", "_");
    }

    private static String shorten(@Nullable String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        return Stream.of(input.toLowerCase().split("_"))
                .filter(word -> !word.isEmpty())
                .map(word -> String.valueOf(word.charAt(0)))
                .collect(java.util.stream.Collectors.joining("_"));
    }

    public static SyncToolSpec toSyncToolSpec(ToolCallback toolCallback, @Nullable MimeType mimeType) {
        SharedSyncToolSpec sharedSpec = toSharedSyncToolSpec(toolCallback, mimeType);
        return SyncToolSpec.builder()
                .tool(sharedSpec.tool())
                .callHandler(sharedSpec.sharedCallHandler::apply)
                .build();
    }

    public static McpServerFeatures.AsyncToolSpec toAsyncToolSpec(ToolCallback toolCallback, @Nullable MimeType mimeType) {
        SyncToolSpec syncToolSpec = toSyncToolSpec(toolCallback, mimeType);
        return McpServerFeatures.AsyncToolSpec.builder()
                .tool(syncToolSpec.tool())
                .callHandler((exchange, map) -> Mono
                        .fromCallable(() -> syncToolSpec.callHandler().apply(new McpSyncServerExchange(exchange), map))
                        .subscribeOn(Schedulers.boundedElastic()))
                .build();
    }

    private static SharedSyncToolSpec toSharedSyncToolSpec(ToolCallback toolCallback, @Nullable MimeType mimeType) {
        Tool tool = Tool.builder()
                .name(toolCallback.getToolDefinition().name())
                .description(toolCallback.getToolDefinition().description())
                .inputSchema(JSON_MAPPER, toolCallback.getToolDefinition().inputSchema())
                .build();

        return new SharedSyncToolSpec(tool, (exchangeOrContext, request) -> {
            try {
                String callResult = toolCallback.call(JSON_MAPPER.writeValueAsString(request.arguments()),
                        new ToolContext(Maps.of(TOOL_CONTEXT_MCP_EXCHANGE_KEY, exchangeOrContext)));
                if (mimeType != null && mimeType.toString().startsWith(Content.TYPE_IMAGE)) {
                    Annotations annotations = new Annotations(Lists.of(Role.ASSISTANT), null);
                    List<Content> contents = Lists.of(new ImageContent(annotations, callResult, mimeType.toString()));
                    return new CallToolResult(contents, false, null, null);
                }
                return new CallToolResult(callResult, false);
            } catch (Exception e) {
                return new CallToolResult(e.getMessage(), true);
            }
        });
    }


    private static final class SharedSyncToolSpec {
        private final Tool tool;
        private final BiFunction<Object, CallToolRequest, CallToolResult> sharedCallHandler;

        public SharedSyncToolSpec(Tool tool, BiFunction<Object, CallToolRequest, CallToolResult> sharedCallHandler) {
            this.tool = tool;
            this.sharedCallHandler = sharedCallHandler;
        }

        public Tool tool() {
            return tool;
        }

        public BiFunction<Object, CallToolRequest, CallToolResult> sharedCallHandler() {
            return sharedCallHandler;
        }
    }
}
