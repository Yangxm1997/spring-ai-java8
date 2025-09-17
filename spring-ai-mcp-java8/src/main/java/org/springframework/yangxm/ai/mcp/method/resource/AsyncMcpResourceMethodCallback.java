package org.springframework.yangxm.ai.mcp.method.resource;

import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.ResourceContents;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpAsyncServerExchange;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public final class AsyncMcpResourceMethodCallback
        extends AbstractMcpResourceMethodCallback
        implements BiFunction<McpAsyncServerExchange, ReadResourceRequest, Mono<ReadResourceResult>> {

    private AsyncMcpResourceMethodCallback(Builder builder) {
        super(builder.method, builder.bean, builder.uri, builder.name, builder.description, builder.mimeType,
                builder.resultConverter, builder.uriTemplateManagerFactory, builder.contentType);
        this.validateMethod(this.method);
    }

    @Override
    public Mono<ReadResourceResult> apply(McpAsyncServerExchange exchange, ReadResourceRequest request) {
        if (request == null) {
            return Mono.error(new IllegalArgumentException("Request must not be null"));
        }

        return Mono.defer(() -> {
            try {
                Map<String, String> uriVariableValues = this.uriTemplateManager.extractVariableValues(request.uri());
                if (!this.uriVariables.isEmpty() && uriVariableValues.size() != this.uriVariables.size()) {
                    return Mono.error(new IllegalArgumentException(
                            String.format("Failed to extract all URI variables from request URI: %s." +
                                            "Expected variables: %s, but found: %s",
                                    request.uri(), this.uriVariables, uriVariableValues.keySet()))
                    );
                }

                Object[] args = this.buildArgs(this.method, exchange, request, uriVariableValues);
                this.method.setAccessible(true);
                Object result = this.method.invoke(this.bean, args);

                if (result instanceof Mono<?>) {
                    return ((Mono<?>) result).map(r -> this.resultConverter.convertToReadResourceResult(r,
                            request.uri(), this.mimeType, this.contentType));
                } else {
                    return Mono.just(this.resultConverter.convertToReadResourceResult(result, request.uri(),
                            this.mimeType, this.contentType));
                }
            } catch (Exception e) {
                return Mono.error(new McpResourceMethodException("Error invoking resource method: " + this.method.getName(), e));
            }
        });
    }

    @Override
    protected void validateReturnType(Method method) {
        Class<?> returnType = method.getReturnType();

        boolean validReturnType = ReadResourceResult.class.isAssignableFrom(returnType)
                || List.class.isAssignableFrom(returnType)
                || ResourceContents.class.isAssignableFrom(returnType)
                || String.class.isAssignableFrom(returnType)
                || Mono.class.isAssignableFrom(returnType);

        if (!validReturnType) {
            throw new IllegalArgumentException(
                    String.format("Method must return either ReadResourceResult, List<ResourceContents>, List<String>, " +
                                    "ResourceContents, String, or Mono<T>: %s in %s returns %s",
                            method.getName(), method.getDeclaringClass().getName(), returnType.getName())
            );
        }
    }

    @Override
    protected boolean isExchangeOrContextType(Class<?> paramType) {
        return McpAsyncServerExchange.class.isAssignableFrom(paramType);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractBuilder<Builder, AsyncMcpResourceMethodCallback> {
        public Builder() {
            this.resultConverter = new DefaultMcpReadResourceResultConverter();
        }

        @Override
        public AsyncMcpResourceMethodCallback build() {
            validate();
            return new AsyncMcpResourceMethodCallback(this);
        }
    }
}