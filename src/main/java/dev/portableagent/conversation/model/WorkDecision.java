package dev.portableagent.conversation.model;

import java.util.Objects;
import java.util.UUID;

public record WorkDecision(WorkStart status, UUID token, SavedReply reply) {

    public WorkDecision {
        Objects.requireNonNull(status, "status must not be null");
        if (status == WorkStart.STARTED && token == null) {
            throw new IllegalArgumentException("STARTED decision must have token");
        }
        if (status == WorkStart.READY && reply == null) {
            throw new IllegalArgumentException("READY decision must have reply");
        }
        if (status != WorkStart.READY && reply != null) {
            throw new IllegalArgumentException("Only READY decision can have reply");
        }
        if (status != WorkStart.STARTED && token != null) {
            throw new IllegalArgumentException("Only STARTED decision can have token");
        }
    }

    public static WorkDecision started(UUID token) {
        return new WorkDecision(WorkStart.STARTED, token, null);
    }

    public static WorkDecision ready(SavedReply reply) {
        return new WorkDecision(WorkStart.READY, null, reply);
    }

    public static WorkDecision notStarted(WorkStart status) {
        return new WorkDecision(status, null, null);
    }

    @Override
    public String toString() {
        return "WorkDecision[status=" + status + ", token=[REDACTED], reply=[REDACTED]]";
    }
}
