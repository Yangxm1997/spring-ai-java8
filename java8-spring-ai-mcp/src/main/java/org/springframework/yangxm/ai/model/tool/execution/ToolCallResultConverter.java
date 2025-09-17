package org.springframework.yangxm.ai.model.tool.execution;

import org.springframework.lang.Nullable;

import java.lang.reflect.Type;

@SuppressWarnings("unused")
@FunctionalInterface
public interface ToolCallResultConverter {
    String convert(@Nullable Object result, @Nullable Type returnType);
}