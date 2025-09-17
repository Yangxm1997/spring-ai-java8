package org.springframework.yangxm.ai.mcp.provider.resource;

import io.modelcontextprotocol.yangxm.ai.mcp.logger.Logger;
import io.modelcontextprotocol.yangxm.ai.mcp.logger.LoggerFactoryHolder;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.Resource;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.AsyncResourceSpec;
import org.reactivestreams.Publisher;
import org.springframework.yangxm.ai.mcp.annotation.McpResource;
import org.springframework.yangxm.ai.mcp.method.resource.AsyncMcpResourceMethodCallback;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AsyncMcpResourceProvider extends AbstractMcpResourceProvider {
    private static final Logger logger = LoggerFactoryHolder.getLogger(AsyncMcpResourceProvider.class);

    public AsyncMcpResourceProvider(List<Object> resourceObjects) {
        super(resourceObjects);
    }

    public List<AsyncResourceSpec> getResourceSpecs() {
        List<AsyncResourceSpec> resourceSpecs = this.resourceObjects.stream()
                .map(resourceObject -> Stream.of(doGetClassMethods(resourceObject))
                        .filter(method -> method.isAnnotationPresent(McpResource.class))
                        .filter(method -> Mono.class.isAssignableFrom(method.getReturnType())
                                || Flux.class.isAssignableFrom(method.getReturnType())
                                || Publisher.class.isAssignableFrom(method.getReturnType()))
                        .sorted(Comparator.comparing(Method::getName))
                        .map(mcpResourceMethod -> {
                            McpResource resourceAnnotation = doGetMcpResourceAnnotation(mcpResourceMethod);
                            String uri = resourceAnnotation.uri();
                            String name = getName(mcpResourceMethod, resourceAnnotation);
                            String description = resourceAnnotation.description();
                            String mimeType = resourceAnnotation.mimeType();
                            Resource mcpResource = Resource.builder()
                                    .uri(uri)
                                    .name(name)
                                    .description(description)
                                    .mimeType(mimeType)
                                    .build();

                            AsyncMcpResourceMethodCallback methodCallback = AsyncMcpResourceMethodCallback.builder()
                                    .method(mcpResourceMethod)
                                    .bean(resourceObject)
                                    .resource(mcpResource)
                                    .build();

                            return AsyncResourceSpec.builder()
                                    .resource(mcpResource)
                                    .readHandler(methodCallback)
                                    .build();
                        })
                        .collect(Collectors.toList()))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        if (resourceSpecs.isEmpty()) {
            logger.warn("No resource methods found in the provided resource objects: {}", this.resourceObjects);
        }
        return resourceSpecs;
    }
}