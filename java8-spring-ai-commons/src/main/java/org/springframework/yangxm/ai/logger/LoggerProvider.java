package org.springframework.yangxm.ai.logger;

@FunctionalInterface
public interface LoggerProvider {
    Logger getLogger(Class<?> clazz);

    LoggerProvider DEFAULT = clazz -> new Logger(org.slf4j.LoggerFactory.getLogger(clazz), clazz);
}
