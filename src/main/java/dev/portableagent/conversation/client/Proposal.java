package dev.portableagent.conversation.client;

import java.util.Map;
import java.util.Objects;

public record Proposal(String kind, String connector, Map<String, Object> payload) {

    public Proposal {
        kind = requireText(kind, "kind");
        connector = requireText(connector, "connector");
        payload = Map.copyOf(Objects.requireNonNull(payload, "payload must not be null"));
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    @Override
    public String toString() {
        return "Proposal[kind=" + kind + ", connector=" + connector + ", payload=[REDACTED]]";
    }
}
