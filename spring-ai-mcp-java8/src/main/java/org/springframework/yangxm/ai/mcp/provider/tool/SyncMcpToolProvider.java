package org.springframework.yangxm.ai.mcp.provider.tool;

import io.modelcontextprotocol.yangxm.ai.mcp.logger.Logger;
import io.modelcontextprotocol.yangxm.ai.mcp.logger.LoggerFactoryHolder;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.Tool;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.SyncToolSpec;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpSyncServerExchange;
import org.springframework.yangxm.ai.mcp.annotation.McpTool;
import org.springframework.yangxm.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.yangxm.ai.mcp.provider.ProviderUtils;
import org.springframework.yangxm.ai.util.Utils;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class SyncMcpToolProvider extends AbstractMcpToolProvider {
    private static final Logger logger = LoggerFactoryHolder.getLogger(SyncMcpToolProvider.class);

    public SyncMcpToolProvider(List<Object> toolObjects) {
        super(toolObjects);
    }

    public List<SyncToolSpec> getToolSpecifications() {

        List<SyncToolSpec> toolSpecs = this.toolObjects.stream()
                .map(toolObject -> Stream.of(this.doGetClassMethods(toolObject))
                        .filter(method -> method.isAnnotationPresent(McpTool.class))
                        .filter(ProviderUtils.isNotReactiveReturnType)
                        .sorted((m1, m2) -> m1.getName().compareTo(m2.getName()))
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

                            // Tool annotations
                            if (toolJavaAnnotation.annotations() != null) {
                                McpAnnotations toolAnnotations = toolJavaAnnotation.annotations();
                                toolBuilder.annotations(new McpSchema.ToolAnnotations(toolAnnotations.title(),
                                        toolAnnotations.readOnlyHint(), toolAnnotations.destructiveHint(),
                                        toolAnnotations.idempotentHint(), toolAnnotations.openWorldHint(), null));

                                // If not provided, the name should be used for display (except
                                // for Tool, where annotations.title should be given precedence
                                // over using name, if present).
                                if (!Utils.hasText(title)) {
                                    title = toolAnnotations.title();
                                }
                            }

                            // If not provided, the name should be used for display (except
                            // for Tool, where annotations.title should be given precedence
                            // over using name, if present).
                            if (!Utils.hasText(title)) {
                                title = toolName;
                            }
                            toolBuilder.title(title);

                            // Generate Output Schema from the method return type.
                            // Output schema is not generated for primitive types, void,
                            // CallToolResult, simple value types (String, etc.)
                            // or if generateOutputSchema attribute is set to false.
                            Class<?> methodReturnType = mcpToolMethod.getReturnType();
                            if (toolJavaAnnotation.generateOutputSchema() && methodReturnType != null
                                    && methodReturnType != McpSchema.CallToolResult.class && methodReturnType != Void.class
                                    && methodReturnType != void.class && !ClassUtils.isPrimitiveOrWrapper(methodReturnType)
                                    && !ClassUtils.isSimpleValueType(methodReturnType)) {

                                toolBuilder.outputSchema(this.getJsonMapper(),
                                        JsonSchemaGenerator.generateFromType(mcpToolMethod.getGenericReturnType()));
                            }

                            Tool tool = toolBuilder.build();

                            boolean useStructuredOtput = tool.outputSchema() != null;

                            ReturnMode returnMode = useStructuredOtput ? ReturnMode.STRUCTURED
                                    : (methodReturnType == Void.TYPE || methodReturnType == void.class ? ReturnMode.VOID
                                    : ReturnMode.TEXT);

                            BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> methodCallback = new SyncMcpToolMethodCallback(
                                    returnMode, mcpToolMethod, toolObject, this.doGetToolCallException());

                            return SyncToolSpec.builder()
                                    .tool(tool)
                                    .callHandler(methodCallback)
                                    .build();
                        })
                        .toList())
                .flatMap(List::stream)
                .toList();

        if (toolSpecs.isEmpty()) {
            logger.warn("No tool methods found in the provided tool objects: {}", this.toolObjects);
        }

        return toolSpecs;
    }
}
