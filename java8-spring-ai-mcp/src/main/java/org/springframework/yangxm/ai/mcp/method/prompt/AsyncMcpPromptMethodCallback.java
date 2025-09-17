package org.springframework.yangxm.ai.mcp.method.prompt;

import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.GetPromptResult;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpAsyncServerExchange;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

public final class AsyncMcpPromptMethodCallback
        extends AbstractMcpPromptMethodCallback
        implements BiFunction<McpAsyncServerExchange, GetPromptRequest, Mono<GetPromptResult>> {

    private AsyncMcpPromptMethodCallback(Builder builder) {
        super(builder.method, builder.bean, builder.prompt);
    }

    @Override
    public Mono<GetPromptResult> apply(McpAsyncServerExchange exchange, GetPromptRequest request) {
        if (request == null) {
            return Mono.error(new IllegalArgumentException("Request must not be null"));
        }

        return Mono.defer(() -> {
            try {
                Object[] args = this.buildArgs(this.method, exchange, request);
                this.method.setAccessible(true);
                Object result = this.method.invoke(this.bean, args);

                if (result instanceof Mono<?>) {
                    return ((Mono<?>) result).map(this::convertToGetPromptResult);
                } else {
                    return Mono.just(convertToGetPromptResult(result));
                }
            } catch (Exception e) {
                return Mono.error(new McpPromptMethodException("Error invoking prompt method: " + this.method.getName(), e));
            }
        });
    }

    @Override
    protected boolean isExchangeOrContextType(Class<?> paramType) {
        return McpAsyncServerExchange.class.isAssignableFrom(paramType);
    }

    @Override
    protected void validateReturnType(Method method) {
        Class<?> returnType = method.getReturnType();
        if (!Mono.class.isAssignableFrom(returnType)) {
            throw new IllegalArgumentException(
                    String.format("Method must return a Mono<T> where T is one of GetPromptResult, List<PromptMessage>, " +
                                    "List<String>, PromptMessage, or String: %s in %s returns %s",
                            method.getName(), method.getDeclaringClass().getName(), returnType.getName())
            );
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractBuilder<Builder, AsyncMcpPromptMethodCallback> {
        @Override
        public AsyncMcpPromptMethodCallback build() {
            validate();
            return new AsyncMcpPromptMethodCallback(this);
        }
    }
}
