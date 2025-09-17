package org.springframework.yangxm.ai.mcp.provider.resource;

import io.modelcontextprotocol.yangxm.ai.mcp.json.McpJsonMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import org.springframework.yangxm.ai.mcp.annotation.McpResource;

import java.lang.reflect.Method;
import java.util.List;

public abstract class AbstractMcpResourceProvider {
    protected final List<Object> resourceObjects;

    protected McpJsonMapper jsonMapper = McpJsonMapper.createDefault();

    public AbstractMcpResourceProvider(List<Object> resourceObjects) {
        Assert.notNull(resourceObjects, "resourceObjects cannot be null");
        this.resourceObjects = resourceObjects;
    }

    protected Method[] doGetClassMethods(Object bean) {
        return bean.getClass().getDeclaredMethods();
    }

    protected McpResource doGetMcpResourceAnnotation(Method method) {
        return method.getAnnotation(McpResource.class);
    }

    public void setJsonMapper(McpJsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public McpJsonMapper getJsonMapper() {
        return this.jsonMapper;
    }

    static String getName(Method method, McpResource resource) {
        Assert.notNull(method, "method cannot be null");
        if (resource == null || StringUtils.isBlank(resource.name())) {
            return method.getName();
        }
        return resource.name();
    }
}
