package org.springframework.yangxm.ai.model.chat.messages;

import org.springframework.yangxm.ai.content.Content;

public interface Message extends Content {
    MessageType getMessageType();
}
