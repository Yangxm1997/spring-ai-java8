package org.springframework.yangxm.ai.mcp.method.tool;

import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.CallToolRequest;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.CallToolResult;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpSyncServerExchange;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

@SuppressWarnings("unused")
public final class SyncMcpToolMethodCallback
        extends AbstractSyncMcpToolMethodCallback<McpSyncServerExchange>
        implements BiFunction<McpSyncServerExchange, CallToolRequest, CallToolResult> {

    public SyncMcpToolMethodCallback(ReturnMode returnMode,
                                     Method toolMethod,
                                     Object toolObject) {
        super(returnMode, toolMethod, toolObject, Exception.class);
    }

    public SyncMcpToolMethodCallback(ReturnMode returnMode,
                                     Method toolMethod,
                                     Object toolObject,
                                     Class<? extends Throwable> toolCallExceptionClass) {
        super(returnMode, toolMethod, toolObject, toolCallExceptionClass);
    }

    @Override
    protected boolean isExchangeOrContextType(Class<?> paramType) {
        return McpSyncServerExchange.class.isAssignableFrom(paramType);
    }

    @Override
    public CallToolResult apply(McpSyncServerExchange exchange, CallToolRequest request) {
        validateRequest(request);
        try {
            Object[] args = this.buildMethodArguments(exchange, request.arguments(), request);
            Object result = this.callMethod(args);
            return this.processResult(result);
        } catch (Exception e) {
            if (this.toolCallExceptionClass.isInstance(e)) {
                return this.createErrorResult(e);
            }
            throw e;
        }
    }
}
