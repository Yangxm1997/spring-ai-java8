package org.springframework.yangxm.ai.tool.execution;

@FunctionalInterface
public interface ToolExecutionExceptionProcessor {
    String process(ToolExecutionException exception);
}
