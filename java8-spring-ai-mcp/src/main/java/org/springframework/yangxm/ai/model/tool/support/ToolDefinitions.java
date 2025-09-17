package org.springframework.yangxm.ai.model.tool.support;

import org.springframework.util.Assert;
import org.springframework.yangxm.ai.mcp.method.tool.utils.JsonSchemaGenerator;
import org.springframework.yangxm.ai.model.tool.definition.DefaultToolDefinition;
import org.springframework.yangxm.ai.model.tool.definition.ToolDefinition;

import java.lang.reflect.Method;

@SuppressWarnings("unused")
public final class ToolDefinitions {
    private ToolDefinitions() {
    }

    public static DefaultToolDefinition.Builder builder(Method method) {
        Assert.notNull(method, "method cannot be null");
        return DefaultToolDefinition.builder()
                .name(ToolUtils.getToolName(method))
                .description(ToolUtils.getToolDescription(method))
                .inputSchema(JsonSchemaGenerator.generateForMethodInput(method));
    }

    public static ToolDefinition from(Method method) {
        return builder(method).build();
    }
}
