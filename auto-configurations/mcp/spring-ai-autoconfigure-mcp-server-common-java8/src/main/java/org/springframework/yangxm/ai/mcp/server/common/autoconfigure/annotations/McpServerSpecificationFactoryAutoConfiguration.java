package org.springframework.yangxm.ai.mcp.server.common.autoconfigure.annotations;

import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.SyncToolSpec;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.yangxm.ai.mcp.annotation.McpTool;
import org.springframework.yangxm.ai.mcp.annotation.spring.SyncMcpAnnotationProviders;
import org.springframework.yangxm.ai.mcp.server.common.autoconfigure.McpServerAutoConfiguration;
import org.springframework.yangxm.ai.mcp.server.common.autoconfigure.annotations.McpServerAnnotationScannerAutoConfiguration.ServerMcpAnnotatedBeans;
import org.springframework.yangxm.ai.mcp.server.common.autoconfigure.properties.McpServerAnnotationScannerProperties;
import org.springframework.yangxm.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;

import java.util.List;

@AutoConfiguration(after = McpServerAnnotationScannerAutoConfiguration.class)
@ConditionalOnClass(McpTool.class)
@ConditionalOnProperty(
        prefix = McpServerAnnotationScannerProperties.CONFIG_PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Conditional(McpServerAutoConfiguration.NonStatelessServerCondition.class)
public class McpServerSpecificationFactoryAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = McpServerProperties.CONFIG_PREFIX,
            name = "type",
            havingValue = "SYNC",
            matchIfMissing = true
    )
    static class SyncServerSpecificationConfiguration {
        @Bean
        public List<SyncToolSpec> toolSpecs(ServerMcpAnnotatedBeans beansWithMcpMethodAnnotations) {
            return SyncMcpAnnotationProviders.toolSpecs(beansWithMcpMethodAnnotations.getBeansByAnnotation(McpTool.class));
        }
    }
}
