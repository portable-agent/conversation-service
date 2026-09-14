package dev.portableagent.conversation.exception;

import java.util.UUID;

public class MessageNotFound extends RuntimeException {

    public MessageNotFound(UUID messageId) {
        super("Message was not found: " + messageId);
    }
}
