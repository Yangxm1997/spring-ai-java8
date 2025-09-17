package org.springframework.yangxm.ai.mcp.provider.tool;

import io.modelcontextprotocol.yangxm.ai.mcp.logger.Logger;
import io.modelcontextprotocol.yangxm.ai.mcp.logger.LoggerFactoryHolder;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.CallToolRequest;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.CallToolResult;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.Tool;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.ToolAnnotations;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.SyncToolSpec;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpSyncServerExchange;
import org.springframework.yangxm.ai.mcp.annotation.McpTool;
import org.springframework.yangxm.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.yangxm.ai.mcp.method.tool.ReturnMode;
import org.springframework.yangxm.ai.mcp.method.tool.SyncMcpToolMethodCallback;
import org.springframework.yangxm.ai.mcp.method.tool.utils.ClassUtils;
import org.springframework.yangxm.ai.mcp.method.tool.utils.JsonSchemaGenerator;
import org.springframework.yangxm.ai.mcp.provider.ProviderUtils;
import org.springframework.yangxm.ai.util.Utils;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SyncMcpToolProvider extends AbstractMcpToolProvider {
    private static final Logger logger = LoggerFactoryHolder.getLogger(SyncMcpToolProvider.class);

    public SyncMcpToolProvider(List<Object> toolObjects) {
        super(toolObjects);
    }

    public List<SyncToolSpec> getToolSpecs() {
        List<SyncToolSpec> toolSpecs = this.toolObjects.stream()
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
                            Tool.Builder toolBuilder = Tool.builder()
                                    .name(toolName)
                                    .description(toolDescription)
                                    .inputSchema(this.getJsonMapper(), inputSchema);
                            String title = toolJavaAnnotation.title();

                            if (toolJavaAnnotation.annotations() != null) {
                                McpAnnotations toolAnnotations = toolJavaAnnotation.annotations();
                                toolBuilder.annotations(
                                        new ToolAnnotations(
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

                            Class<?> methodReturnType = mcpToolMethod.getReturnType();
                            if (toolJavaAnnotation.generateOutputSchema()
                                    && methodReturnType != CallToolResult.class
                                    && methodReturnType != Void.class
                                    && methodReturnType != void.class
                                    && !ClassUtils.isPrimitiveOrWrapper(methodReturnType)
                                    && !ClassUtils.isSimpleValueType(methodReturnType)) {

                                toolBuilder.outputSchema(
                                        this.getJsonMapper(),
                                        JsonSchemaGenerator.generateFromType(mcpToolMethod.getGenericReturnType())
                                );
                            }

                            Tool tool = toolBuilder.build();

                            boolean useStructuredOutput = tool.outputSchema() != null;
                            ReturnMode returnMode = useStructuredOutput ?
                                    ReturnMode.STRUCTURED :
                                    (methodReturnType == void.class ? ReturnMode.VOID : ReturnMode.TEXT);

                            BiFunction<McpSyncServerExchange, CallToolRequest, CallToolResult> methodCallback =
                                    new SyncMcpToolMethodCallback(
                                            returnMode, mcpToolMethod, toolObject, this.doGetToolCallException()
                                    );

                            return SyncToolSpec.builder()
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
