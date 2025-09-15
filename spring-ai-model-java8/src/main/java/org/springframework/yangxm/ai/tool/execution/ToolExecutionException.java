package org.springframework.yangxm.ai.tool.execution;

import org.springframework.yangxm.ai.tool.definition.ToolDefinition;

public class ToolExecutionException extends RuntimeException {
    private final ToolDefinition toolDefinition;

    public ToolExecutionException(ToolDefinition toolDefinition, Throwable cause) {
        super(cause.getMessage(), cause);
        this.toolDefinition = toolDefinition;
    }

    public ToolDefinition getToolDefinition() {
        return this.toolDefinition;
    }
}