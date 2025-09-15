package org.springframework.yangxm.ai.tool.definition;

public interface ToolDefinition {
    String name();

    String description();

    String inputSchema();

    static DefaultToolDefinition.Builder builder() {
        return DefaultToolDefinition.builder();
    }
}
