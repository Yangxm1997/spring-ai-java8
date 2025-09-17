package org.springframework.yangxm.ai.mcp.method.resource;

import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.ReadResourceResult;
import org.springframework.yangxm.ai.mcp.method.resource.AbstractMcpResourceMethodCallback.ContentType;

public interface McpReadResourceResultConverter {
    ReadResourceResult convertToReadResourceResult(Object result, String requestUri, String mimeType, ContentType contentType);
}
