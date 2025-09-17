package org.springframework.yangxm.ai.mcp.provider.tool;

import io.modelcontextprotocol.yangxm.ai.mcp.logger.Logger;
import io.modelcontextprotocol.yangxm.ai.mcp.logger.LoggerFactoryHolder;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.CallToolRequest;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.CallToolResult;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpAsyncServerExchange;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.AsyncToolSpec;
import org.springframework.yangxm.ai.mcp.annotation.McpTool;
import org.springframework.yangxm.ai.mcp.method.tool.AsyncMcpToolMethodCallback;
import org.springframework.yangxm.ai.mcp.method.tool.ReturnMode;
import org.springframework.yangxm.ai.mcp.method.tool.utils.ClassUtils;
import org.springframework.yangxm.ai.mcp.method.tool.utils.JsonSchemaGenerator;
import org.springframework.yangxm.ai.mcp.method.tool.utils.ReactiveUtils;
import org.springframework.yangxm.ai.mcp.provider.ProviderUtils;
import org.springframework.yangxm.ai.util.Utils;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AsyncMcpToolProvider extends AbstractMcpToolProvider {
    private static final Logger logger = LoggerFactoryHolder.getLogger(SyncMcpToolProvider.class);

    public AsyncMcpToolProvider(List<Object> toolObjects) {
        super(toolObjects);
    }

    public List<AsyncToolSpec> getToolSpecs() {
        List<AsyncToolSpec> toolSpecs = this.toolObjects.stream()
                .map(toolObject -> Stream.of(this.doGetClassMethods(toolObject))
                        .filter(method -> method.isAnnotationPresent(McpTool.class))
                        .filter(ProviderUtils.isNotReactiveReturnType)
                        .sorted(Comparator.comparing(Method::getName))
                        .map(mcpToolMethod -> {
                            McpTool toolJavaAnnotation = this.doGetMcpToolAnnotation(mcpToolMethod);
                            String toolName = Utils.hasText(toolJavaAnnotation.name()) ?
                                    toolJavaAnnotation.name() : mcpToolMethod.getName();
                            String toolDescription = toolJavaAnnotation.description();
                            String inputSchema = JsonSchemaGenerator.generateForMethodInput(mcpToolMethod);
                            McpSchema.Tool.Builder toolBuilder = McpSchema.Tool.builder()
                                    .name(toolName)
                                    .description(toolDescription)
                                    .inputSchema(this.getJsonMapper(), inputSchema);
                            String title = toolJavaAnnotation.title();

                            if (toolJavaAnnotation.annotations() != null) {
                                McpTool.McpAnnotations toolAnnotations = toolJavaAnnotation.annotations();
                                toolBuilder.annotations(
                                        new McpSchema.ToolAnnotations(
                                                toolAnnotations.title(),
                                                toolAnnotations.readOnlyHint(),
                                                toolAnnotations.destructiveHint(),
                                                toolAnnotations.idempotentHint(),
                                                toolAnnotations.openWorldHint(),
                                                null)
                                );

                                if (!Utils.hasText(title)) {
                                    title = toolAnnotations.title();
                                }
                            }

                            if (!Utils.hasText(title)) {
                                title = toolName;
                            }
                            toolBuilder.title(title);

                            if (toolJavaAnnotation.generateOutputSchema()
                                    && !ReactiveUtils.isReactiveReturnTypeOfVoid(mcpToolMethod)
                                    && !ReactiveUtils.isReactiveReturnTypeOfCallToolResult(mcpToolMethod)) {
                                ReactiveUtils.getReactiveReturnTypeArgument(mcpToolMethod).ifPresent(typeArgument -> {
                                    Class<?> methodReturnType = typeArgument instanceof Class<?> ? (Class<?>) typeArgument : null;
                                    if (!ClassUtils.isPrimitiveOrWrapper(methodReturnType)
                                            && !ClassUtils.isSimpleValueType(methodReturnType)) {
                                        toolBuilder.outputSchema(this.getJsonMapper(),
                                                JsonSchemaGenerator.generateFromClass(typeArgument.getClass())
                                        );
                                    }
                                });
                            }

                            McpSchema.Tool tool = toolBuilder.build();
                            ReturnMode returnMode = tool.outputSchema() != null ? ReturnMode.STRUCTURED
                                    : ReactiveUtils.isReactiveReturnTypeOfVoid(mcpToolMethod) ? ReturnMode.VOID
                                    : ReturnMode.TEXT;

                            BiFunction<McpAsyncServerExchange, CallToolRequest, Mono<CallToolResult>> methodCallback =
                                    new AsyncMcpToolMethodCallback(
                                            returnMode, mcpToolMethod, toolObject, this.doGetToolCallException()
                                    );

                            return AsyncToolSpec.builder()
                                    .tool(tool)
                                    .callHandler(methodCallback)
                                    .build();
                        })
                        .collect(Collectors.toList()))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        if (toolSpecs.isEmpty()) {
            logger.warn("No tool methods found in the provided tool objects: {}", this.toolObjects);
        }
        return toolSpecs;
    }
}
