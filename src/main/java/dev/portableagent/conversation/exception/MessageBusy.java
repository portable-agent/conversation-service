package dev.portableagent.conversation.exception;

import java.util.UUID;

public class MessageBusy extends RuntimeException {

    public MessageBusy(UUID messageId) {
        super("Message is already being processed: " + messageId);
    }
}
