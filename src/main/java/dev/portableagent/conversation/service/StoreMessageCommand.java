package dev.portableagent.conversation.service;

import java.util.UUID;

public record StoreMessageCommand(
        UUID tenantId,
        String subject,
        UUID conversationId,
        String requestKey,
        String text,
        String locale,
        String timeZone) {

    @Override
    public String toString() {
        return "StoreMessageCommand[tenantId="
                + tenantId
                + ", conversationId="
                + conversationId
                + ", requestKey="
                + requestKey
                + ", text=[REDACTED]]";
    }
}
