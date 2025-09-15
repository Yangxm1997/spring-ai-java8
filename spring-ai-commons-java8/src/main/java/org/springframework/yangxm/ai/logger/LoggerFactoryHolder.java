package org.springframework.yangxm.ai.logger;

import org.springframework.lang.Nullable;

public final class LoggerFactoryHolder {
    private static LoggerProvider provider = LoggerProvider.DEFAULT;

    private LoggerFactoryHolder() {
    }

    public static void setProvider(@Nullable LoggerProvider loggerProvider) {
        if (loggerProvider != null) {
            provider = loggerProvider;
        }
    }

    public static Logger getLogger(Class<?> clazz) {
        return provider.getLogger(clazz);
    }
}
