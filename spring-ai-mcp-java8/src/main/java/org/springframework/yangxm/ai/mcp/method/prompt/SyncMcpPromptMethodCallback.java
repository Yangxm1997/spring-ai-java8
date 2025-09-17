package org.springframework.yangxm.ai.mcp.method.prompt;

import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.GetPromptResult;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.PromptMessage;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpSyncServerExchange;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.BiFunction;

public final class SyncMcpPromptMethodCallback
        extends AbstractMcpPromptMethodCallback
        implements BiFunction<McpSyncServerExchange, GetPromptRequest, GetPromptResult> {

    private SyncMcpPromptMethodCallback(Builder builder) {
        super(builder.method, builder.bean, builder.prompt);
    }

    @Override
    public GetPromptResult apply(McpSyncServerExchange exchange, GetPromptRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }

        try {
            Object[] args = this.buildArgs(this.method, exchange, request);
            this.method.setAccessible(true);
            Object result = this.method.invoke(this.bean, args);
            return this.convertToGetPromptResult(result);
        } catch (Exception e) {
            throw new McpPromptMethodException("Error invoking prompt method: " + this.method.getName(), e);
        }
    }

    @Override
    protected boolean isExchangeOrContextType(Class<?> paramType) {
        return McpSyncServerExchange.class.isAssignableFrom(paramType);
    }

    @Override
    protected void validateReturnType(Method method) {
        Class<?> returnType = method.getReturnType();

        boolean validReturnType = GetPromptResult.class.isAssignableFrom(returnType)
                || List.class.isAssignableFrom(returnType)
                || PromptMessage.class.isAssignableFrom(returnType)
                || String.class.isAssignableFrom(returnType);

        if (!validReturnType) {
            throw new IllegalArgumentException(
                    String.format("Method must return either GetPromptResult, List<PromptMessage>, List<String>, " +
                                    "PromptMessage, or String: %s in %s returns %s",
                            method.getName(), method.getDeclaringClass().getName(), returnType.getName())
            );
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractBuilder<Builder, SyncMcpPromptMethodCallback> {
        @Override
        public SyncMcpPromptMethodCallback build() {
            validate();
            return new SyncMcpPromptMethodCallback(this);
        }
    }
}
