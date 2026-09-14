package dev.portableagent.conversation.model;

import java.util.Map;
import java.util.Objects;

public record SavedReply(ReplyType type, Map<String, Object> data) {

    public SavedReply {
        Objects.requireNonNull(type, "type must not be null");
        data = Map.copyOf(Objects.requireNonNull(data, "data must not be null"));
    }

    @Override
    public String toString() {
        return "SavedReply[type=" + type + ", data=[REDACTED]]";
    }
}
