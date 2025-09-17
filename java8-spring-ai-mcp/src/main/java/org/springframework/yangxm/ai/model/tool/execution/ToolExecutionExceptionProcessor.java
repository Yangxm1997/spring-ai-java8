package org.springframework.yangxm.ai.model.tool.execution;

@SuppressWarnings("unused")
@FunctionalInterface
public interface ToolExecutionExceptionProcessor {
    String process(ToolExecutionException exception);
}
