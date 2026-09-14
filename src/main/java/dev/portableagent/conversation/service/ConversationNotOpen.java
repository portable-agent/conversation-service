package dev.portableagent.conversation.service;

import java.util.UUID;

public class ConversationNotOpen extends RuntimeException {

    public ConversationNotOpen(UUID conversationId) {
        super("Conversation is not open: " + conversationId);
    }
}
