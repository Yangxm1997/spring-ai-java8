package org.springframework.yangxm.ai.util;

import org.springframework.lang.Nullable;

public abstract class Utils {
    private Utils() {
    }

    public static boolean hasText(@Nullable String str) {
        return str != null && !str.trim().isEmpty();
    }
}
