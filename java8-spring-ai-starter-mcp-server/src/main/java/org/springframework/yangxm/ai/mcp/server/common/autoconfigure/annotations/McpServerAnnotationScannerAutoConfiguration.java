package org.springframework.yangxm.ai.mcp.server.common.autoconfigure.annotations;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.yangxm.ai.mcp.annotation.McpComplete;
import org.springframework.yangxm.ai.mcp.annotation.McpPrompt;
import org.springframework.yangxm.ai.mcp.annotation.McpResource;
import org.springframework.yangxm.ai.mcp.annotation.McpTool;
import org.springframework.yangxm.ai.mcp.annotation.spring.scan.AbstractAnnotatedMethodBeanPostProcessor;
import org.springframework.yangxm.ai.mcp.annotation.spring.scan.AbstractMcpAnnotatedBeans;
import org.springframework.yangxm.ai.mcp.server.common.autoconfigure.properties.McpServerAnnotationScannerProperties;
import org.springframework.yangxm.ai.util.Sets;

import java.lang.annotation.Annotation;
import java.util.Set;

@SuppressWarnings("unused")
@AutoConfiguration
@ConditionalOnClass(McpTool.class)
@ConditionalOnProperty(
        prefix = McpServerAnnotationScannerProperties.CONFIG_PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableConfigurationProperties(McpServerAnnotationScannerProperties.class)
public class McpServerAnnotationScannerAutoConfiguration {
    private static final Set<Class<? extends Annotation>> SERVER_MCP_ANNOTATIONS = Sets.of(
            McpTool.class, McpResource.class, McpPrompt.class, McpComplete.class
    );

    @Bean
    @ConditionalOnMissingBean
    public ServerMcpAnnotatedBeans serverAnnotatedBeanRegistry() {
        return new ServerMcpAnnotatedBeans();
    }

    @Bean
    @ConditionalOnMissingBean
    public ServerAnnotatedMethodBeanPostProcessor serverAnnotatedMethodBeanPostProcessor(
            ServerMcpAnnotatedBeans serverMcpAnnotatedBeans,
            McpServerAnnotationScannerProperties properties) {
        return new ServerAnnotatedMethodBeanPostProcessor(serverMcpAnnotatedBeans, SERVER_MCP_ANNOTATIONS);
    }

    public static class ServerMcpAnnotatedBeans extends AbstractMcpAnnotatedBeans {
    }

    public static class ServerAnnotatedMethodBeanPostProcessor extends AbstractAnnotatedMethodBeanPostProcessor {
        public ServerAnnotatedMethodBeanPostProcessor(ServerMcpAnnotatedBeans serverMcpAnnotatedBeans,
                                                      Set<Class<? extends Annotation>> targetAnnotations) {
            super(serverMcpAnnotatedBeans, targetAnnotations);
        }
    }
}
