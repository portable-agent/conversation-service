package dev.portableagent.conversation.service;

import dev.portableagent.conversation.model.SavedReply;
import java.util.Objects;
import java.util.UUID;

public record MessageResult(UUID messageId, UUID conversationId, SavedReply reply) {

    public MessageResult {
        Objects.requireNonNull(messageId, "messageId must not be null");
        Objects.requireNonNull(conversationId, "conversationId must not be null");
        Objects.requireNonNull(reply, "reply must not be null");
    }
}
