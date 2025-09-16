package org.springframework.yangxm.ai.util;

import org.springframework.lang.Nullable;

import java.util.Collection;

public final class Assert {
    private Assert() {
    }

    public static void notEmpty(@Nullable Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notNull(@Nullable Object object, String message) {
        if (null == object) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void hasText(String text, String message) {
        if (!hasText(text)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static boolean hasText(@Nullable String str) {
        return (str != null && !str.trim().isEmpty());
    }
}
