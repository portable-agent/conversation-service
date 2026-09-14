package dev.portableagent.conversation.model;

import java.time.Instant;
import java.util.UUID;

public record Message(
        UUID id,
        UUID conversationId,
        UUID tenantId,
        String subject,
        String requestKey,
        String text,
        String locale,
        String timeZone,
        Instant createdAt,
        Instant erasedAt) {

    @Override
    public String toString() {
        return "Message[id=" + id + ", conversationId=" + conversationId + ", text=[REDACTED]]";
    }
}
