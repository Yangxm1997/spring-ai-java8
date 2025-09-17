package org.springframework.yangxm.ai.model.tool.definition;

@SuppressWarnings("unused")
public interface ToolDefinition {
    String name();

    String description();

    String inputSchema();

    static DefaultToolDefinition.Builder builder() {
        return DefaultToolDefinition.builder();
    }
}
