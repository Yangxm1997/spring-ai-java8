package org.springframework.yangxm.ai.util;

import org.springframework.lang.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class Sets {
    private Sets() {
    }

    @SafeVarargs
    private static <E> Set<E> setN(@Nullable E... elements) {
        if (elements == null || elements.length == 0) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(elements)));
    }

    public static <E> Set<E> of() {
        return setN();
    }

    public static <E> Set<E> of(E e1) {
        return setN(e1);
    }

    public static <E> Set<E> of(E e1, E e2) {
        return setN(e1, e2);
    }

    public static <E> Set<E> of(E e1, E e2, E e3) {
        return setN(e1, e2, e3);
    }

    public static <E> Set<E> of(E e1, E e2, E e3, E e4) {
        return setN(e1, e2, e3, e4);
    }

    public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5) {
        return setN(e1, e2, e3, e4, e5);
    }
}
