package org.springframework.yangxm.ai.mcp.provider.prompt;

import org.springframework.util.Assert;
import org.springframework.yangxm.ai.mcp.annotation.McpPrompt;
import org.springframework.yangxm.ai.mcp.annotation.McpResource;

import java.lang.reflect.Method;
import java.util.List;

public abstract class AbstractMcpPromptProvider {
    protected final List<Object> promptObjects;

    public AbstractMcpPromptProvider(List<Object> promptObjects) {
        Assert.notNull(promptObjects, "promptObjects cannot be null");
        this.promptObjects = promptObjects;
    }

    protected Method[] doGetClassMethods(Object bean) {
        return bean.getClass().getDeclaredMethods();
    }
}