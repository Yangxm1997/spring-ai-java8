package org.springframework.yangxm.ai.mcp;

import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema;

import java.util.function.BiPredicate;

public interface McpToolFilter extends BiPredicate<McpConnectionInfo, McpSchema.Tool> {
}
