package org.springframework.yangxm.ai.mcp.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpTool {
    String name() default "";

    String description() default "";

    McpAnnotations annotations() default @McpTool.McpAnnotations;

    boolean generateOutputSchema() default true;

    String title() default "";

    boolean enabled() default true;

    String baseUrl() default "";

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.ANNOTATION_TYPE})
    @interface McpAnnotations {
        String title() default "";

        boolean readOnlyHint() default false;

        boolean destructiveHint() default true;

        boolean idempotentHint() default false;

        boolean openWorldHint() default true;
    }
}
