package dev.portableagent.conversation.client;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public record SavedAction(UUID id, String payloadHash, Map<String, Object> payload) {

    private static final Pattern SHA_256 = Pattern.compile("[a-f0-9]{64}");

    public SavedAction {
        Objects.requireNonNull(id, "id must not be null");
        if (payloadHash == null || !SHA_256.matcher(payloadHash).matches()) {
            throw new IllegalArgumentException("payloadHash must be a SHA-256 hex string");
        }
        payload = Map.copyOf(Objects.requireNonNull(payload, "payload must not be null"));
    }

    @Override
    public String toString() {
        return "SavedAction[id=" + id + ", payloadHash=" + payloadHash + ", payload=[REDACTED]]";
    }
}
