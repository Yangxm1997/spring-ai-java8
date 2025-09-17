package org.springframework.yangxm.ai.mcp.method.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.CallToolRequest;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.CallToolResult;
import org.springframework.yangxm.ai.mcp.annotation.McpMeta;
import org.springframework.yangxm.ai.mcp.annotation.McpProgressToken;
import org.springframework.yangxm.ai.util.JsonParser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.stream.Stream;

public abstract class AbstractSyncMcpToolMethodCallback<T> {
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<Map<String, Object>>() {
    };
    protected final ReturnMode returnMode;
    protected final Method toolMethod;
    protected final Object toolObject;
    protected final Class<? extends Throwable> toolCallExceptionClass;


    protected AbstractSyncMcpToolMethodCallback(ReturnMode returnMode,
                                                Method toolMethod,
                                                Object toolObject,
                                                Class<? extends Throwable> toolCallExceptionClass) {
        this.returnMode = returnMode;
        this.toolMethod = toolMethod;
        this.toolObject = toolObject;
        this.toolCallExceptionClass = toolCallExceptionClass;
    }

    /**
     * 调用方法
     */
    protected Object callMethod(Object[] methodArguments) {
        this.toolMethod.setAccessible(true);

        Object result;
        try {
            result = this.toolMethod.invoke(this.toolObject, methodArguments);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Could not access method: " + ex.getMessage(), ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException("Error invoking method: " + this.toolMethod.getName(), ex);
        }
        return result;
    }

    /**
     * 构建方法参数
     */
    protected Object[] buildMethodArguments(T exchangeOrContext,
                                            Map<String, Object> toolInputArguments,
                                            CallToolRequest request) {
        return Stream.of(this.toolMethod.getParameters()).map(parameter -> {
            if (parameter.isAnnotationPresent(McpProgressToken.class)) {
                return request != null ? request.progressToken() : null;
            }

            if (McpMeta.class.isAssignableFrom(parameter.getType())) {
                return request != null ? new McpMeta(request.meta()) : new McpMeta(null);
            }

            if (CallToolRequest.class.isAssignableFrom(parameter.getType())) {
                return request;
            }

            if (isExchangeOrContextType(parameter.getType())) {
                return exchangeOrContext;
            }

            Object rawArgument = toolInputArguments.get(parameter.getName());
            return buildTypedArgument(rawArgument, parameter.getParameterizedType());
        }).toArray();
    }

    /**
     * 构建参数类型
     */
    protected Object buildTypedArgument(Object value, Type type) {
        if (value == null) {
            return null;
        }

        if (type instanceof Class<?>) {
            return JsonParser.toTypedObject(value, (Class<?>) type);
        }

        String json = JsonParser.toJson(value);
        return JsonParser.fromJson(json, type);
    }

    /**
     * 处理返回结果
     */
    protected CallToolResult processResult(Object result) {
        if (result instanceof CallToolResult) {
            return (CallToolResult) result;
        }

        Type returnType = this.toolMethod.getGenericReturnType();

        if (returnMode == ReturnMode.VOID || returnType == void.class) {
            return CallToolResult.builder()
                    .addTextContent(JsonParser.toJson("Done"))
                    .build();
        }

        if (this.returnMode == ReturnMode.STRUCTURED) {
            String jsonOutput = JsonParser.toJson(result);
            Map<String, Object> structuredOutput = JsonParser.fromJson(jsonOutput, MAP_TYPE_REFERENCE);
            return CallToolResult.builder()
                    .structuredContent(structuredOutput)
                    .build();
        }

        if (result == null) {
            return CallToolResult.builder().addTextContent("null").build();
        }

        if (result instanceof String) {
            return CallToolResult.builder()
                    .addTextContent((String) result)
                    .build();
        }

        return CallToolResult.builder()
                .addTextContent(JsonParser.toJson(result))
                .build();
    }

    /**
     * 创建错误结果
     */
    protected CallToolResult createErrorResult(Exception e) {
        return CallToolResult.builder()
                .isError(true)
                .addTextContent(String.format("Error invoking method: %s", e.getMessage()))
                .build();
    }

    /**
     * 判断结果是否为null
     */
    protected void validateRequest(CallToolRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }
    }

    protected abstract boolean isExchangeOrContextType(Class<?> paramType);
}