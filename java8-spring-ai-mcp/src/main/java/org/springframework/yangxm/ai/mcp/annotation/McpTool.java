package org.springframework.yangxm.ai.mcp.annotation;

import org.springframework.lang.Nullable;
import org.springframework.yangxm.ai.model.tool.execution.DefaultToolCallResultConverter;
import org.springframework.yangxm.ai.model.tool.execution.ToolCallResultConverter;

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

    @Nullable
    McpAnnotations annotations() default @McpTool.McpAnnotations;

    boolean generateOutputSchema() default true;

    String title() default "";

    boolean returnDirect() default false;

    Class<? extends ToolCallResultConverter> resultConverter() default DefaultToolCallResultConverter.class;


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
