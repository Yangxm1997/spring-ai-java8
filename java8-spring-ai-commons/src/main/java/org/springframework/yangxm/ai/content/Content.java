package org.springframework.yangxm.ai.content;

import java.util.Map;

public interface Content {
    String getText();

    Map<String, Object> getMetadata();
}
