package org.springframework.yangxm.ai.mcp.server.common.autoconfigure;


import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.AsyncToolSpec;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.SyncToolSpec;
import io.modelcontextprotocol.yangxm.ai.mcp.util.Lists;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.yangxm.ai.mcp.McpToolUtils;
import org.springframework.yangxm.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;
import org.springframework.yangxm.ai.model.tool.ToolCallback;
import org.springframework.yangxm.ai.model.tool.ToolCallbackProvider;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@AutoConfiguration
@EnableConfigurationProperties(McpServerProperties.class)
@Conditional({
        ToolCallbackConverterAutoConfiguration.ToolCallbackConverterCondition.class,
        McpServerAutoConfiguration.NonStatelessServerCondition.class
})
public class ToolCallbackConverterAutoConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = McpServerProperties.CONFIG_PREFIX,
            name = "type",
            havingValue = "SYNC",
            matchIfMissing = true
    )
    public List<SyncToolSpec> syncToolSpecs(ObjectProvider<List<ToolCallback>> toolCalls,
                                            List<ToolCallback> toolCallbacksList,
                                            List<ToolCallbackProvider> toolCallbackProvider,
                                            McpServerProperties serverProperties) {
        List<ToolCallback> tools = this.aggregateToolCallbacks(toolCalls, toolCallbacksList, toolCallbackProvider);
        return this.toSyncToolSpecs(tools, serverProperties);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = McpServerProperties.CONFIG_PREFIX,
            name = "type",
            havingValue = "ASYNC"
    )
    public List<AsyncToolSpec> asyncToolSpecs(ObjectProvider<List<ToolCallback>> toolCalls,
                                              List<ToolCallback> toolCallbacksList,
                                              List<ToolCallbackProvider> toolCallbackProvider,
                                              McpServerProperties serverProperties) {
        List<ToolCallback> tools = this.aggregateToolCallbacks(toolCalls, toolCallbacksList, toolCallbackProvider);
        return this.toAsyncToolSpecs(tools, serverProperties);
    }

    private List<SyncToolSpec> toSyncToolSpecs(List<ToolCallback> tools, McpServerProperties serverProperties) {
        return tools.stream()
                .collect(Collectors.toMap(
                        tool -> tool.getToolDefinition().name(),
                        tool -> tool,
                        (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .map(tool -> {
                    String toolName = tool.getToolDefinition().name();
                    MimeType mimeType = (serverProperties.getToolResponseMimeType().containsKey(toolName))
                            ? MimeType.valueOf(serverProperties.getToolResponseMimeType().get(toolName)) : null;
                    return McpToolUtils.toSyncToolSpec(tool, mimeType);
                })
                .collect(Collectors.toList());
    }

    private List<AsyncToolSpec> toAsyncToolSpecs(List<ToolCallback> tools, McpServerProperties serverProperties) {
        return tools.stream()
                .collect(Collectors.toMap(
                        tool -> tool.getToolDefinition().name(),
                        tool -> tool,
                        (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .map(tool -> {
                    String toolName = tool.getToolDefinition().name();
                    MimeType mimeType = (serverProperties.getToolResponseMimeType().containsKey(toolName))
                            ? MimeType.valueOf(serverProperties.getToolResponseMimeType().get(toolName)) : null;
                    return McpToolUtils.toAsyncToolSpec(tool, mimeType);
                })
                .collect(Collectors.toList());
    }

    private List<ToolCallback> aggregateToolCallbacks(ObjectProvider<List<ToolCallback>> toolCalls,
                                                      List<ToolCallback> toolCallbacksList,
                                                      List<ToolCallbackProvider> toolCallbackProvider) {
        List<ToolCallback> tools = toolCalls.stream().flatMap(List::stream).collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(toolCallbacksList)) {
            tools.addAll(toolCallbacksList);
        }

        List<ToolCallback> providerToolCallbacks = toolCallbackProvider.stream()
                .map(pr -> Lists.of(pr.getToolCallbacks()))
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        tools.addAll(providerToolCallbacks);
        return tools;
    }

    public static class ToolCallbackConverterCondition extends AllNestedConditions {
        public ToolCallbackConverterCondition() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnProperty(
                prefix = McpServerProperties.CONFIG_PREFIX,
                name = "enabled",
                havingValue = "true",
                matchIfMissing = true
        )
        static class McpServerEnabledCondition {
        }

        @ConditionalOnProperty(
                prefix = McpServerProperties.CONFIG_PREFIX,
                name = "tool-callback-converter",
                havingValue = "true",
                matchIfMissing = true
        )
        static class ToolCallbackConvertCondition {
        }
    }
}