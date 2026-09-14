package dev.portableagent.conversation.exception;

import java.util.UUID;

public class MessageWorkChanged extends RuntimeException {

    public MessageWorkChanged(UUID messageId) {
        super("Message work was changed: " + messageId);
    }
}
