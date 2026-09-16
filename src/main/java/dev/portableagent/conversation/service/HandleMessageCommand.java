package dev.portableagent.conversation.service;

import java.util.Objects;

public record HandleMessageCommand(StoreMessageCommand storeCommand, String accessToken) {

    public HandleMessageCommand {
        Objects.requireNonNull(storeCommand, "storeCommand must not be null");
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken must not be blank");
        }
    }

    @Override
    public String toString() {
        return "HandleMessageCommand[storeCommand=" + storeCommand + ", accessToken=[REDACTED]]";
    }
}
