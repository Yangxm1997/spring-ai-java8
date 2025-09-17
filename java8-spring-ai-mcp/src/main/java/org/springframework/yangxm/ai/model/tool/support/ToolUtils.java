package org.springframework.yangxm.ai.model.tool.support;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.yangxm.ai.logger.Logger;
import org.springframework.yangxm.ai.logger.LoggerFactoryHolder;
import org.springframework.yangxm.ai.mcp.annotation.McpTool;
import org.springframework.yangxm.ai.model.tool.ToolCallback;
import org.springframework.yangxm.ai.model.tool.execution.DefaultToolCallResultConverter;
import org.springframework.yangxm.ai.model.tool.execution.ToolCallResultConverter;
import org.springframework.yangxm.ai.util.ParsingUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public final class ToolUtils {
    private static final Logger logger = LoggerFactoryHolder.getLogger(ToolUtils.class);

    private static final Pattern RECOMMENDED_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\.-]+$");

    private ToolUtils() {
    }

    public static String getToolName(Method method) {
        Assert.notNull(method, "method cannot be null");
        McpTool toolAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, McpTool.class);
        String toolName;
        if (toolAnnotation == null) {
            toolName = method.getName();
        } else {
            toolName = StringUtils.hasText(toolAnnotation.name()) ? toolAnnotation.name() : method.getName();
        }
        validateToolName(toolName);
        return toolName;
    }

    public static String getToolDescriptionFromName(String toolName) {
        Assert.hasText(toolName, "toolName cannot be null or empty");
        return ParsingUtils.reConcatenateCamelCase(toolName, " ");
    }

    public static String getToolDescription(Method method) {
        Assert.notNull(method, "method cannot be null");
        McpTool toolAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, McpTool.class);
        if (toolAnnotation == null) {
            return ParsingUtils.reConcatenateCamelCase(method.getName(), " ");
        }
        return StringUtils.hasText(toolAnnotation.description()) ? toolAnnotation.description() : method.getName();
    }

    public static boolean getToolReturnDirect(Method method) {
        Assert.notNull(method, "method cannot be null");
        McpTool toolAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, McpTool.class);
        return toolAnnotation != null && toolAnnotation.returnDirect();
    }

    public static ToolCallResultConverter getToolCallResultConverter(Method method) {
        Assert.notNull(method, "method cannot be null");
        McpTool toolAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, McpTool.class);
        if (toolAnnotation == null) {
            return new DefaultToolCallResultConverter();
        }
        Class<? extends ToolCallResultConverter> type = toolAnnotation.resultConverter();
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to instantiate ToolCallResultConverter: " + type, e);
        }
    }

    public static List<String> getDuplicateToolNames(List<ToolCallback> toolCallbacks) {
        Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
        return toolCallbacks.stream()
                .collect(Collectors.groupingBy(toolCallback -> toolCallback.getToolDefinition().name(),
                        Collectors.counting()))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public static List<String> getDuplicateToolNames(ToolCallback... toolCallbacks) {
        Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
        return getDuplicateToolNames(Arrays.asList(toolCallbacks));
    }

    /**
     * Validates that a tool name follows recommended naming conventions. Logs a warning
     * if the tool name contains characters that may not be compatible with some LLMs.
     *
     * @param toolName the tool name to validate
     */
    private static void validateToolName(String toolName) {
        Assert.hasText(toolName, "Tool name cannot be null or empty");
        if (!RECOMMENDED_NAME_PATTERN.matcher(toolName).matches()) {
            logger.warn("Tool name '{}' may not be compatible with some LLMs (e.g., OpenAI). "
                    + "Consider using only alphanumeric characters, underscores, hyphens, and dots.", toolName);
        }
    }

}