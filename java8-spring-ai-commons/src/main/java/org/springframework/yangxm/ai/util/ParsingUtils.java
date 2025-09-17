package org.springframework.yangxm.ai.util;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class ParsingUtils {
    private ParsingUtils() {
    }

    private static final String UPPER = "\\p{Lu}|\\P{InBASIC_LATIN}";
    private static final String LOWER = "\\p{Ll}";
    private static final String CAMEL_CASE_REGEX = "(?<!(^|[%u_$]))(?=[%u])|(?<!^)(?=[%u][%l])".replace("%u", UPPER).replace("%l", LOWER);
    private static final Pattern CAMEL_CASE = Pattern.compile(CAMEL_CASE_REGEX);

    public static List<String> splitCamelCase(String source) {
        return split(source, false);
    }

    public static List<String> splitCamelCaseToLower(String source) {
        return split(source, true);
    }

    public static String reConcatenateCamelCase(String source, String delimiter) {
        Assert.notNull(source, "Source string must not be null");
        Assert.notNull(delimiter, "Delimiter must not be null");
        return StringUtils.collectionToDelimitedString(splitCamelCaseToLower(source), delimiter);
    }

    private static List<String> split(String source, boolean toLower) {
        Assert.notNull(source, "Source string must not be null");
        String[] parts = CAMEL_CASE.split(source);
        List<String> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            result.add(toLower ? part.toLowerCase() : part);
        }
        return Collections.unmodifiableList(result);
    }
}
