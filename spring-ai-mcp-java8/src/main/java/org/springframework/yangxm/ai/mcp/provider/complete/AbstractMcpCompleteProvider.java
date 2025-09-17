package org.springframework.yangxm.ai.mcp.provider.complete;

import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.List;

public abstract class AbstractMcpCompleteProvider {
    protected final List<Object> completeObjects;

    public AbstractMcpCompleteProvider(List<Object> completeObjects) {
        Assert.notNull(completeObjects, "completeObjects cannot be null");
        this.completeObjects = completeObjects;
    }

    protected Method[] doGetClassMethods(Object bean) {
        return bean.getClass().getDeclaredMethods();
    }
}