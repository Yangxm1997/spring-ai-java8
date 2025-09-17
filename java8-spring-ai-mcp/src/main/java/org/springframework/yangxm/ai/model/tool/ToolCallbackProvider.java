package org.springframework.yangxm.ai.model.tool;

import java.util.List;

@SuppressWarnings("unused")
public interface ToolCallbackProvider {

    ToolCallback[] getToolCallbacks();

    static ToolCallbackProvider from(List<? extends ToolCallback> toolCallbacks) {
        return new StaticToolCallbackProvider(toolCallbacks);
    }

    static ToolCallbackProvider from(ToolCallback... toolCallbacks) {
        return new StaticToolCallbackProvider(toolCallbacks);
    }
}
