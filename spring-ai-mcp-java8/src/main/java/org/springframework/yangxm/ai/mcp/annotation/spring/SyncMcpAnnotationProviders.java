package org.springframework.yangxm.ai.mcp.annotation.spring;

import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.SyncCompletionSpec;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.SyncPromptSpec;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.SyncResourceSpec;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.SyncToolSpec;
import org.springframework.yangxm.ai.mcp.provider.complete.SyncMcpCompleteProvider;
import org.springframework.yangxm.ai.mcp.provider.prompt.SyncMcpPromptProvider;
import org.springframework.yangxm.ai.mcp.provider.resource.SyncMcpResourceProvider;
import org.springframework.yangxm.ai.mcp.provider.tool.SyncMcpToolProvider;

import java.lang.reflect.Method;
import java.util.List;

public final class SyncMcpAnnotationProviders {
    private SyncMcpAnnotationProviders() {
    }

    // TOOLS
    public static List<SyncToolSpec> toolSpecs(List<Object> toolObjects) {
        return new SpringAiSyncToolProvider(toolObjects).getToolSpecs();
    }

    // RESOURCE
    public static List<SyncResourceSpec> resourceSpecs(List<Object> resourceObjects) {
        return new SpringAiSyncMcpResourceProvider(resourceObjects).getResourceSpecs();
    }

    // PROMPT
    public static List<SyncPromptSpec> promptSpecs(List<Object> promptObjects) {
        return new SpringAiSyncMcpPromptProvider(promptObjects).getPromptSpecs();
    }

    // COMPLETE
    public static List<SyncCompletionSpec> completeSpecs(List<Object> completeObjects) {
        return new SpringAiSyncMcpCompleteProvider(completeObjects).getCompleteSpecs();
    }


    // TOOL
    private final static class SpringAiSyncToolProvider extends SyncMcpToolProvider {
        private SpringAiSyncToolProvider(List<Object> toolObjects) {
            super(toolObjects);
        }

        @Override
        protected Method[] doGetClassMethods(Object bean) {
            return AnnotationProviderUtil.beanMethods(bean);
        }
    }

    // RESOURCE
    private final static class SpringAiSyncMcpResourceProvider extends SyncMcpResourceProvider {
        private SpringAiSyncMcpResourceProvider(List<Object> resourceObjects) {
            super(resourceObjects);
        }

        @Override
        protected Method[] doGetClassMethods(Object bean) {
            return AnnotationProviderUtil.beanMethods(bean);
        }
    }

    // PROMPT
    private final static class SpringAiSyncMcpPromptProvider extends SyncMcpPromptProvider {
        private SpringAiSyncMcpPromptProvider(List<Object> promptObjects) {
            super(promptObjects);
        }

        @Override
        protected Method[] doGetClassMethods(Object bean) {
            return AnnotationProviderUtil.beanMethods(bean);
        }
    }

    // COMPLETE
    private final static class SpringAiSyncMcpCompleteProvider extends SyncMcpCompleteProvider {
        private SpringAiSyncMcpCompleteProvider(List<Object> completeObjects) {
            super(completeObjects);
        }

        @Override
        protected Method[] doGetClassMethods(Object bean) {
            return AnnotationProviderUtil.beanMethods(bean);
        }
    }
}
