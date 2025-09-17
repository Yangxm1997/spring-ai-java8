package org.springframework.yangxm.ai.mcp.annotation.spring;

import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.SyncResourceSpec;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.SyncToolSpec;
import org.springframework.yangxm.ai.mcp.provider.tool.SyncMcpToolProvider;

import java.lang.reflect.Method;
import java.util.List;

public final class SyncMcpAnnotationProviders {
    private SyncMcpAnnotationProviders() {
    }

    // TOOLS
    public static List<SyncToolSpec> toolSpecifications(List<Object> toolObjects) {
        return new SpringAiSyncToolProvider(toolObjects).getToolSpecifications();
    }

    // RESOURCE
    public static List<SyncResourceSpec> resourceSpecifications(List<Object> resourceObjects) {
        return new SpringAiSyncMcpResourceProvider(resourceObjects).getResourceSpecifications();
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
}
