package org.springframework.yangxm.ai.tool.annotation;

import org.springframework.yangxm.ai.tool.execution.DefaultToolCallResultConverter;
import org.springframework.yangxm.ai.tool.execution.ToolCallResultConverter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Tool {
    String name() default "";

    String description() default "";

    boolean returnDirect() default false;

    Class<? extends ToolCallResultConverter> resultConverter() default DefaultToolCallResultConverter.class;
}
