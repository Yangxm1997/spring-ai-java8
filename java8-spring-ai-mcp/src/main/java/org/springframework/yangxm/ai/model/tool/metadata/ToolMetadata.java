package org.springframework.yangxm.ai.model.tool.metadata;

import org.springframework.util.Assert;
import org.springframework.yangxm.ai.model.tool.support.ToolUtils;

import java.lang.reflect.Method;

@SuppressWarnings("unused")
public interface ToolMetadata {
    default boolean returnDirect() {
        return false;
    }

    static DefaultToolMetadata.Builder builder() {
        return DefaultToolMetadata.builder();
    }

    static ToolMetadata from(Method method) {
        Assert.notNull(method, "method cannot be null");
        return DefaultToolMetadata.builder().returnDirect(ToolUtils.getToolReturnDirect(method)).build();
    }
}
