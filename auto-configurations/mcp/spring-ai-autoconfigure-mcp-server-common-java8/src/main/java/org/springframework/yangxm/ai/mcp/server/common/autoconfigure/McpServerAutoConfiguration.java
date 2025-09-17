package org.springframework.yangxm.ai.mcp.server.common.autoconfigure;

import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.Implementation;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.Root;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpAsyncServer;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpAsyncServerExchange;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.AsyncCompletionSpec;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.AsyncPromptSpec;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.AsyncResourceSpec;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.AsyncToolSpec;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.SyncCompletionSpec;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.SyncPromptSpec;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.SyncResourceSpec;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.SyncToolSpec;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerSessionTransportProvider;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerTransportProvider;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpSyncServer;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpSyncServerExchange;
import io.modelcontextprotocol.yangxm.ai.mcp.server.transport.StdioServerTransportProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.support.StandardServletEnvironment;
import org.springframework.yangxm.ai.logger.Logger;
import org.springframework.yangxm.ai.logger.LoggerFactoryHolder;
import org.springframework.yangxm.ai.mcp.server.common.autoconfigure.properties.McpServerChangeNotificationProperties;
import org.springframework.yangxm.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@AutoConfiguration(
        afterName = {
                "org.springframework.ai.mcp.server.common.autoconfigure.ToolCallbackConverterAutoConfiguration",
                "org.springframework.ai.mcp.server.autoconfigure.McpServerSseWebFluxAutoConfiguration",
                "org.springframework.ai.mcp.server.autoconfigure.McpServerSseWebMvcAutoConfiguration",
                "org.springframework.ai.mcp.server.autoconfigure.McpServerStreamableHttpWebMvcAutoConfiguration",
                "org.springframework.ai.mcp.server.autoconfigure.McpServerStreamableHttpWebFluxAutoConfiguration"}
)
@ConditionalOnClass({McpSchema.class})
@EnableConfigurationProperties({McpServerProperties.class, McpServerChangeNotificationProperties.class})
@ConditionalOnProperty(
        prefix = McpServerProperties.CONFIG_PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Conditional(McpServerAutoConfiguration.NonStatelessServerCondition.class)
public class McpServerAutoConfiguration {
    private static final Logger logger = LoggerFactoryHolder.getLogger(McpServerAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public McpServerTransportProvider stdioServerTransport() {
        return new StdioServerTransportProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public ServerCapabilities.Builder capabilitiesBuilder() {
        return ServerCapabilities.builder();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = McpServerProperties.CONFIG_PREFIX,
            name = "type",
            havingValue = "SYNC",
            matchIfMissing = true
    )
    public McpSyncServer mcpSyncServer(McpServerTransportProvider transportProvider,
                                       ServerCapabilities.Builder capabilitiesBuilder, McpServerProperties serverProperties,
                                       McpServerChangeNotificationProperties changeNotificationProperties,
                                       ObjectProvider<List<SyncToolSpec>> tools,
                                       ObjectProvider<List<SyncResourceSpec>> resources,
                                       ObjectProvider<List<SyncPromptSpec>> prompts,
                                       ObjectProvider<List<SyncCompletionSpec>> completions,
                                       ObjectProvider<BiConsumer<McpSyncServerExchange, List<Root>>> rootsChangeConsumers,
                                       Environment environment) {
        Implementation serverInfo = new Implementation(serverProperties.getName(), serverProperties.getVersion());
        McpSyncServer.Builder serverBuilder = McpSyncServer.builder();
        serverBuilder.serverInfo(serverInfo);

        // Tools
        if (serverProperties.getCapabilities().isTool()) {
            boolean notification = changeNotificationProperties.isToolChangeNotification();
            logger.info("[Sync] Enable tools capabilities, notification: {}", notification);
            capabilitiesBuilder.tools(notification);
            List<SyncToolSpec> toolSpecs = tools.stream().flatMap(List::stream).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(toolSpecs)) {
                serverBuilder.toolSpecs(toolSpecs);
                logger.info("[Sync] Registered tools: {}", toolSpecs.size());
            }
        }

        // Resources
        if (serverProperties.getCapabilities().isResource()) {
            boolean notification = changeNotificationProperties.isResourceChangeNotification();
            logger.info("[Sync] Enable resources capabilities, notification: {}", notification);
            capabilitiesBuilder.resources(false, notification);
            List<SyncResourceSpec> resourceSpecs = resources.stream().flatMap(List::stream).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(resourceSpecs)) {
                serverBuilder.resourceSpecs(resourceSpecs);
                logger.info("[Sync] Registered resources: {}", resourceSpecs.size());
            }
        }

        // Prompts
        if (serverProperties.getCapabilities().isPrompt()) {
            boolean notification = changeNotificationProperties.isPromptChangeNotification();
            logger.info("[Sync] Enable prompts capabilities, notification: {}", notification);
            capabilitiesBuilder.prompts(notification);
            List<SyncPromptSpec> promptSpecs = prompts.stream().flatMap(List::stream).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(promptSpecs)) {
                serverBuilder.promptSpecs(promptSpecs);
                logger.info("[Sync] Registered prompts: {}", promptSpecs.size());
            }
        }

        // Completions
        if (serverProperties.getCapabilities().isCompletion()) {
            logger.info("[Sync] Enable completions capabilities");
            capabilitiesBuilder.completions();
            List<SyncCompletionSpec> completionSpecs = completions.stream().flatMap(List::stream).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(completionSpecs)) {
                serverBuilder.completionSpecs(completionSpecs);
                logger.info("[Sync] Registered completions: {}", completionSpecs.size());
            }
        }

        rootsChangeConsumers.ifAvailable(consumer -> {
            serverBuilder.rootsChangeConsumers(consumer);
            logger.info("[Sync] Registered roots change consumer");
        });

        serverBuilder.serverCapabilities(capabilitiesBuilder.build());
        serverBuilder.instructions(serverProperties.getInstructions());
        serverBuilder.requestTimeout(serverProperties.getRequestTimeout());
        if (environment instanceof StandardServletEnvironment) {
            serverBuilder.immediateExecution(true);
        }

        // TODO McpStreamableServer
        return serverBuilder.buildSingleSessionMcpServer((McpServerSessionTransportProvider) transportProvider);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = McpServerProperties.CONFIG_PREFIX,
            name = "type",
            havingValue = "ASYNC"
    )
    public McpAsyncServer mcpAsyncServer(McpServerTransportProvider transportProvider,
                                         ServerCapabilities.Builder capabilitiesBuilder, McpServerProperties serverProperties,
                                         McpServerChangeNotificationProperties changeNotificationProperties,
                                         ObjectProvider<List<AsyncToolSpec>> tools,
                                         ObjectProvider<List<AsyncResourceSpec>> resources,
                                         ObjectProvider<List<AsyncPromptSpec>> prompts,
                                         ObjectProvider<List<AsyncCompletionSpec>> completions,
                                         ObjectProvider<BiConsumer<McpAsyncServerExchange, List<McpSchema.Root>>> rootsChangeConsumer) {
        Implementation serverInfo = new Implementation(serverProperties.getName(), serverProperties.getVersion());
        McpAsyncServer.Builder serverBuilder = McpAsyncServer.builder();
        serverBuilder.serverInfo(serverInfo);

        // Tools
        if (serverProperties.getCapabilities().isTool()) {
            boolean notification = changeNotificationProperties.isToolChangeNotification();
            logger.info("[Async] Enable tools capabilities, notification: {}", notification);
            capabilitiesBuilder.tools(notification);
            List<AsyncToolSpec> toolSpecs = tools.stream().flatMap(List::stream).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(toolSpecs)) {
                serverBuilder.toolSpecs(toolSpecs);
                logger.info("[Async] Registered tools: {}", toolSpecs.size());
            }
        }

        // Resources
        if (serverProperties.getCapabilities().isResource()) {
            boolean notification = changeNotificationProperties.isResourceChangeNotification();
            logger.info("[Async] Enable resources capabilities, notification: {}", notification);
            capabilitiesBuilder.resources(false, notification);
            List<AsyncResourceSpec> resourceSpecs = resources.stream().flatMap(List::stream).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(resourceSpecs)) {
                serverBuilder.resourceSpecs(resourceSpecs);
                logger.info("[Async] Registered resources: {}", resourceSpecs.size());
            }
        }

        // Prompts
        if (serverProperties.getCapabilities().isPrompt()) {
            boolean notification = changeNotificationProperties.isPromptChangeNotification();
            logger.info("[Async] Enable prompts capabilities, notification: {}", notification);
            capabilitiesBuilder.prompts(notification);
            List<AsyncPromptSpec> promptSpecs = prompts.stream().flatMap(List::stream).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(promptSpecs)) {
                serverBuilder.promptSpecs(promptSpecs);
                logger.info("[Async] Registered prompts: {}", promptSpecs.size());
            }
        }

        // Completions
        if (serverProperties.getCapabilities().isCompletion()) {
            logger.info("[Async] Enable completions capabilities");
            capabilitiesBuilder.completions();
            List<AsyncCompletionSpec> completionSpecs = completions.stream().flatMap(List::stream).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(completionSpecs)) {
                serverBuilder.completionSpecs(completionSpecs);
                logger.info("[Async] Registered completions: {}", completionSpecs.size());
            }
        }

        rootsChangeConsumer.ifAvailable(consumer -> {
            BiFunction<McpAsyncServerExchange, List<Root>, Mono<Void>> asyncConsumer = (exchange, roots) -> {
                consumer.accept(exchange, roots);
                return Mono.empty();
            };
            serverBuilder.rootsChangeConsumers(asyncConsumer);
            logger.info("[Async] Registered roots change consumer");
        });

        serverBuilder.serverCapabilities(capabilitiesBuilder.build());
        serverBuilder.instructions(serverProperties.getInstructions());
        serverBuilder.requestTimeout(serverProperties.getRequestTimeout());

        // TODO McpStreamableServer
        return serverBuilder.buildSingleSessionMcpServer((McpServerSessionTransportProvider) transportProvider);
    }


    public static class NonStatelessServerCondition extends AnyNestedCondition {
        public NonStatelessServerCondition() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnProperty(
                prefix = McpServerProperties.CONFIG_PREFIX,
                name = "protocol",
                havingValue = "SSE",
                matchIfMissing = true
        )
        static class SseEnabledCondition {
        }

        @ConditionalOnProperty(
                prefix = McpServerProperties.CONFIG_PREFIX,
                name = "protocol",
                havingValue = "STREAMABLE",
                matchIfMissing = false
        )
        static class StreamableEnabledCondition {
        }
    }

    public static class EnabledSseServerCondition extends AllNestedConditions {
        public EnabledSseServerCondition() {
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

        @ConditionalOnProperty(prefix =
                McpServerProperties.CONFIG_PREFIX,
                name = "protocol",
                havingValue = "SSE",
                matchIfMissing = true
        )
        static class SseEnabledCondition {
        }
    }

    public static class EnabledStreamableServerCondition extends AllNestedConditions {
        public EnabledStreamableServerCondition() {
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
                name = "protocol",
                havingValue = "STREAMABLE",
                matchIfMissing = false
        )
        static class StreamableEnabledCondition {
        }
    }
}
