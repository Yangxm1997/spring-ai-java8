package org.springframework.yangxm.ai.mcp.method.tool;

import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.CallToolRequest;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.CallToolResult;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpAsyncServerExchange;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

@SuppressWarnings("unused")
public final class AsyncMcpToolMethodCallback
        extends AbstractAsyncMcpToolMethodCallback<McpAsyncServerExchange>
        implements BiFunction<McpAsyncServerExchange, CallToolRequest, Mono<CallToolResult>> {

    public AsyncMcpToolMethodCallback(ReturnMode returnMode, Method toolMethod, Object toolObject) {
        super(returnMode, toolMethod, toolObject, Exception.class);
    }

    public AsyncMcpToolMethodCallback(ReturnMode returnMode, Method toolMethod, Object toolObject,
                                      Class<? extends Throwable> toolCallExceptionClass) {
        super(returnMode, toolMethod, toolObject, toolCallExceptionClass);
    }

    @Override
    protected boolean isExchangeOrContextType(Class<?> paramType) {
        return McpAsyncServerExchange.class.isAssignableFrom(paramType);
    }

    @Override
    public Mono<CallToolResult> apply(McpAsyncServerExchange exchange, CallToolRequest request) {
        return validateRequest(request).then(Mono.defer(() -> {
            try {
                Object[] args = this.buildMethodArguments(exchange, request.arguments(), request);
                Object result = this.callMethod(args);
                return this.convertToCallToolResult(result);
            } catch (Exception e) {
                if (this.toolCallExceptionClass.isInstance(e)) {
                    return this.createErrorResult(e);
                }
                return Mono.error(e);
            }
        }));
    }
}
