package dev.portableagent.conversation.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MessageWork(
        UUID messageId,
        WorkStatus status,
        int attempts,
        Instant startedAt,
        UUID token,
        SavedReply reply,
        WorkError error,
        Instant updatedAt) {

    public MessageWork {
        Objects.requireNonNull(messageId, "messageId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (attempts < 0) {
            throw new IllegalArgumentException("attempts must not be negative");
        }
        if (status == WorkStatus.PROCESSING && (startedAt == null || token == null)) {
            throw new IllegalArgumentException("PROCESSING work must have startedAt and token");
        }
        if (status != WorkStatus.PROCESSING && (startedAt != null || token != null)) {
            throw new IllegalArgumentException("Only PROCESSING work can have lease data");
        }
        if (status == WorkStatus.READY && reply == null) {
            throw new IllegalArgumentException("READY work must have reply");
        }
        if (status != WorkStatus.READY && reply != null) {
            throw new IllegalArgumentException("Only READY work can have reply");
        }
        if (status == WorkStatus.FAILED && error == null) {
            throw new IllegalArgumentException("FAILED work must have error");
        }
        if (status != WorkStatus.FAILED && error != null) {
            throw new IllegalArgumentException("Only FAILED work can have error");
        }
    }
}
