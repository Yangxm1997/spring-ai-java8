package org.springframework.yangxm.ai.mcp;

import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;

import java.util.stream.Stream;

public final class McpToolUtils {
    public static final String TOOL_CONTEXT_MCP_EXCHANGE_KEY = "exchange";

    private McpToolUtils() {
    }

    public static String prefixedToolName(String prefix, @Nullable String title, String toolName) {
        if (StringUtils.isEmpty(prefix) || StringUtils.isEmpty(toolName)) {
            throw new IllegalArgumentException("Prefix or toolName cannot be null or empty");
        }

        String input = shorten(format(prefix));
        if (!StringUtils.isEmpty(title)) {
            input = input + "_" + format(title); // Do not shorten the title.
        }

        input = input + "_" + format(toolName);

        if (input.length() > 64) {
            input = input.substring(input.length() - 64);
        }

        return input;
    }

    public static String prefixedToolName(String prefix, String toolName) {
        return prefixedToolName(prefix, null, toolName);
    }

    private static String format(String input) {
        String formatted = input.replaceAll("[^\\p{IsHan}\\p{InCJK_Unified_Ideographs}\\p{InCJK_Compatibility_Ideographs}a-zA-Z0-9_-]", "");
        return formatted.replaceAll("-", "_");
    }

    private static String shorten(@Nullable String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        return Stream.of(input.toLowerCase().split("_"))
                .filter(word -> !word.isEmpty())
                .map(word -> String.valueOf(word.charAt(0)))
                .collect(java.util.stream.Collectors.joining("_"));
    }
}
