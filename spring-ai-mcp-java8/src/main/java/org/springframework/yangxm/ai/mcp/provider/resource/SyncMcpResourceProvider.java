package org.springframework.yangxm.ai.mcp.provider.resource;

import io.modelcontextprotocol.yangxm.ai.mcp.logger.Logger;
import io.modelcontextprotocol.yangxm.ai.mcp.logger.LoggerFactoryHolder;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema;
import io.modelcontextprotocol.yangxm.ai.mcp.schema.McpSchema.Resource;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures;
import io.modelcontextprotocol.yangxm.ai.mcp.server.McpServerFeatures.SyncResourceSpec;
import org.springframework.yangxm.ai.mcp.annotation.McpResource;
import org.springframework.yangxm.ai.mcp.method.resource.SyncMcpResourceMethodCallback;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SyncMcpResourceProvider extends AbstractMcpResourceProvider {
    private static final Logger logger = LoggerFactoryHolder.getLogger(SyncMcpResourceProvider.class);

    public SyncMcpResourceProvider(List<Object> resourceObjects) {
        super(resourceObjects);
    }

    public List<SyncResourceSpec> getResourceSpecs() {
        List<SyncResourceSpec> resourceSpecs = this.resourceObjects.stream()
                .map(resourceObject -> Stream.of(this.doGetClassMethods(resourceObject))
                        .filter(resourceMethod -> resourceMethod.isAnnotationPresent(McpResource.class))
                        .filter(method -> !Mono.class.isAssignableFrom(method.getReturnType()))
                        .sorted(Comparator.comparing(Method::getName))
                        .map(mcpResourceMethod -> {
                            McpResource resourceAnnotation = mcpResourceMethod.getAnnotation(McpResource.class);
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

                            SyncMcpResourceMethodCallback methodCallback = SyncMcpResourceMethodCallback.builder()
                                    .method(mcpResourceMethod)
                                    .bean(resourceObject)
                                    .resource(mcpResource)
                                    .build();

                            return SyncResourceSpec.builder()
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
