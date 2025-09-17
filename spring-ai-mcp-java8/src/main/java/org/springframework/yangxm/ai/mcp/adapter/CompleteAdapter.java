package org.springframework.yangxm.ai.mcp.adapter;

import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.CompleteReference;
import org.apache.commons.lang3.StringUtils;
import org.springframework.yangxm.ai.mcp.annotation.McpComplete;
import org.springframework.yangxm.ai.util.Assert;

import java.lang.reflect.Method;

public final class CompleteAdapter {
    private CompleteAdapter() {
    }

    public static CompleteReference asCompleteReference(McpComplete mcpComplete) {
        Assert.notNull(mcpComplete, "mcpComplete cannot be null");

        String prompt = mcpComplete.prompt();
        String uri = mcpComplete.uri();

        if (StringUtils.isEmpty(prompt) && StringUtils.isEmpty(uri)) {
            throw new IllegalArgumentException("Either prompt or uri must be provided in McpComplete annotation");
        }
        if (StringUtils.isNotEmpty(prompt) && StringUtils.isNotEmpty(uri)) {
            throw new IllegalArgumentException("Only one of prompt or uri can be provided in McpComplete annotation");
        }

        if (StringUtils.isNotEmpty(prompt)) {
            return new McpSchema.PromptReference(prompt);
        } else {
            return new McpSchema.ResourceReference(uri);
        }
    }

    public static CompleteReference asCompleteReference(McpComplete mcpComplete, Method method) {
        Assert.notNull(method, "method cannot be null");
        return asCompleteReference(mcpComplete);
    }
}
