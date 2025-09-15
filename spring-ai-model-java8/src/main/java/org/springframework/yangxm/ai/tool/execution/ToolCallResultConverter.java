package org.springframework.yangxm.ai.tool.execution;

import org.springframework.lang.Nullable;

import java.lang.reflect.Type;

@FunctionalInterface
public interface ToolCallResultConverter {
    String convert(@Nullable Object result, @Nullable Type returnType);
}