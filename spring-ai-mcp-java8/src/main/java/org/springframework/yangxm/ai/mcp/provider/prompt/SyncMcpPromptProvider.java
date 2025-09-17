package org.springframework.yangxm.ai.mcp.provider.prompt;

import io.modelcontextprotocol.yangxm.ai.mcp.logger.Logger;
import io.modelcontextprotocol.yangxm.ai.mcp.logger.LoggerFactoryHolder;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.Prompt;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.SyncPromptSpec;
import org.springframework.yangxm.ai.mcp.adapter.PromptAdapter;
import org.springframework.yangxm.ai.mcp.annotation.McpPrompt;
import org.springframework.yangxm.ai.mcp.method.prompt.SyncMcpPromptMethodCallback;
import org.springframework.yangxm.ai.mcp.provider.resource.SyncMcpResourceProvider;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SyncMcpPromptProvider extends AbstractMcpPromptProvider {
    private static final Logger logger = LoggerFactoryHolder.getLogger(SyncMcpPromptProvider.class);

    public SyncMcpPromptProvider(List<Object> promptObjects) {
        super(promptObjects);
    }

    public List<SyncPromptSpec> getPromptSpecs() {
        List<SyncPromptSpec> promptSpecs = this.promptObjects.stream()
                .map(resourceObject -> Stream.of(doGetClassMethods(resourceObject))
                        .filter(method -> method.isAnnotationPresent(McpPrompt.class))
                        .filter(method -> !Mono.class.isAssignableFrom(method.getReturnType()))
                        .sorted(Comparator.comparing(Method::getName))
                        .map(mcpPromptMethod -> {
                            McpPrompt promptAnnotation = mcpPromptMethod.getAnnotation(McpPrompt.class);
                            Prompt mcpPrompt = PromptAdapter.asPrompt(promptAnnotation, mcpPromptMethod);

                            SyncMcpPromptMethodCallback methodCallback = SyncMcpPromptMethodCallback.builder()
                                    .method(mcpPromptMethod)
                                    .bean(resourceObject)
                                    .prompt(mcpPrompt)
                                    .build();

                            return SyncPromptSpec.builder()
                                    .prompt(mcpPrompt)
                                    .promptHandler(methodCallback)
                                    .build();
                        })
                        .collect(Collectors.toList()))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        if (promptSpecs.isEmpty()) {
            logger.warn("No prompt methods found in the provided prompt objects: {}", this.promptObjects);
        }
        return promptSpecs;
    }
}
