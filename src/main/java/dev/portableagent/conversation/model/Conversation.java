package dev.portableagent.conversation.model;

import java.time.Instant;
import java.util.UUID;

public record Conversation(
        UUID id,
        UUID tenantId,
        String subject,
        ConversationStatus status,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt) {}
