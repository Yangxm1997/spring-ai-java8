package org.springframework.yangxm.ai.tool.execution;

import org.springframework.lang.Nullable;
import org.springframework.yangxm.ai.logger.Logger;
import org.springframework.yangxm.ai.logger.LoggerFactoryHolder;
import org.springframework.yangxm.ai.util.JsonParser;
import org.springframework.yangxm.ai.util.Maps;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Base64;

public final class DefaultToolCallResultConverter implements ToolCallResultConverter {
    private static final Logger logger = LoggerFactoryHolder.getLogger(DefaultToolCallResultConverter.class);

    @Override
    public String convert(@Nullable Object result, @Nullable Type returnType) {
        if (returnType == Void.TYPE) {
            logger.debug("The tool has no return type. Converting to conventional response.");
            return JsonParser.toJson("Done");
        }
        if (result instanceof RenderedImage) {
            final ByteArrayOutputStream buf = new ByteArrayOutputStream(1024 * 4);
            try {
                ImageIO.write((RenderedImage) result, "PNG", buf);
            } catch (IOException e) {
                return "Failed to convert tool result to a base64 image: " + e.getMessage();
            }
            final String imgB64 = Base64.getEncoder().encodeToString(buf.toByteArray());
            return JsonParser.toJson(Maps.of("mimeType", "image/png", "data", imgB64));
        } else {
            logger.debug("Converting tool result to JSON.");
            return JsonParser.toJson(result);
        }
    }
}
