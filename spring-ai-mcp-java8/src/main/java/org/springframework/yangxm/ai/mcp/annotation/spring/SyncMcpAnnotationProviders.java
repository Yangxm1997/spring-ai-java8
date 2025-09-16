package org.springframework.yangxm.ai.mcp.annotation.spring;

import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.SyncToolSpec;

import java.util.List;

public final class SyncMcpAnnotationProviders {
    private SyncMcpAnnotationProviders() {
    }

    // TOOLS
    public static List<SyncToolSpec> toolSpecifications(List<Object> toolObjects) {
        return new SpringAiSyncToolProvider(toolObjects).getToolSpecifications();
    }
}
