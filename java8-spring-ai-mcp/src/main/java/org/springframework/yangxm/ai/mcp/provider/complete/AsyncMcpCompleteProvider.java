package org.springframework.yangxm.ai.mcp.provider.complete;

import io.modelcontextprotocol.yangxm.ai.mcp.logger.Logger;
import io.modelcontextprotocol.yangxm.ai.mcp.logger.LoggerFactoryHolder;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.CompleteReference;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.AsyncCompletionSpec;
import org.reactivestreams.Publisher;
import org.springframework.yangxm.ai.mcp.adapter.CompleteAdapter;
import org.springframework.yangxm.ai.mcp.annotation.McpComplete;
import org.springframework.yangxm.ai.mcp.method.complete.AsyncMcpCompleteMethodCallback;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AsyncMcpCompleteProvider extends AbstractMcpCompleteProvider {
    private static final Logger logger = LoggerFactoryHolder.getLogger(AsyncMcpCompleteProvider.class);

    public AsyncMcpCompleteProvider(List<Object> completeObjects) {
        super(completeObjects);
    }

    public List<AsyncCompletionSpec> getCompleteSpecs() {
        List<AsyncCompletionSpec> completeSpecs = this.completeObjects.stream()
                .map(completeObject -> Stream.of(doGetClassMethods(completeObject))
                        .filter(method -> method.isAnnotationPresent(McpComplete.class))
                        .filter(method -> Mono.class.isAssignableFrom(method.getReturnType())
                                || Flux.class.isAssignableFrom(method.getReturnType())
                                || Publisher.class.isAssignableFrom(method.getReturnType()))
                        .sorted(Comparator.comparing(Method::getName))
                        .map(mcpCompleteMethod -> {
                            McpComplete completeAnnotation = mcpCompleteMethod.getAnnotation(McpComplete.class);
                            CompleteReference completeRef = CompleteAdapter.asCompleteReference(completeAnnotation, mcpCompleteMethod);

                            AsyncMcpCompleteMethodCallback methodCallback = AsyncMcpCompleteMethodCallback.builder()
                                    .method(mcpCompleteMethod)
                                    .bean(completeObject)
                                    .prompt(completeAnnotation.prompt().isEmpty() ? null : completeAnnotation.prompt())
                                    .uri(completeAnnotation.uri().isEmpty() ? null : completeAnnotation.uri())
                                    .build();

                            return AsyncCompletionSpec.builder()
                                    .reference(completeRef)
                                    .completionHandler(methodCallback)
                                    .build();
                        })
                        .collect(Collectors.toList()))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        if (completeSpecs.isEmpty()) {
            logger.warn("No async complete methods found in the provided complete objects: {}", this.completeObjects);
        }
        return completeSpecs;
    }
}