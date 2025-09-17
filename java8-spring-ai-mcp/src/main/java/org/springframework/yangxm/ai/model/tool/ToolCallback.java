package org.springframework.yangxm.ai.model.tool;

import org.springframework.lang.Nullable;
import org.springframework.yangxm.ai.logger.Logger;
import org.springframework.yangxm.ai.logger.LoggerFactoryHolder;
import org.springframework.yangxm.ai.model.chat.model.ToolContext;
import org.springframework.yangxm.ai.model.tool.definition.ToolDefinition;
import org.springframework.yangxm.ai.model.tool.metadata.ToolMetadata;

@SuppressWarnings("unused")
public interface ToolCallback {
    Logger logger = LoggerFactoryHolder.getLogger(ToolCallback.class);

    ToolDefinition getToolDefinition();

    default ToolMetadata getToolMetadata() {
        return ToolMetadata.builder().build();
    }

    String call(String toolInput);

    default String call(String toolInput, @Nullable ToolContext toolContext) {
        if (toolContext != null && !toolContext.getContext().isEmpty()) {
            logger.info("By default the tool context is not used,  "
                    + "override the method 'call(String toolInput, ToolContext toolcon  text)' to support the use of tool context."
                    + "Review the ToolCallback implementation for {}", getToolDefinition().name());
        }
        return call(toolInput);
    }
}
