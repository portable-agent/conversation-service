package dev.portableagent.conversation.exception;

import java.util.UUID;

public class MessageErased extends RuntimeException {

    public MessageErased(UUID messageId) {
        super("Message text was erased: " + messageId);
    }
}
