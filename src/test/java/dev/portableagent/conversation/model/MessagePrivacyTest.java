package dev.portableagent.conversation.model;

import static org.assertj.core.api.Assertions.assertThat;

import dev.portableagent.conversation.service.StoreMessageCommand;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MessagePrivacyTest {

    private static final String RAW_TEXT = "Секретный текст пользователя";

    @Test
    void toString_shouldNeverContainRawText() {
        var tenantId = UUID.randomUUID();
        var conversationId = UUID.randomUUID();
        var command = new StoreMessageCommand(
                tenantId, "user-42", conversationId, "request-1", RAW_TEXT, "ru-RU", "Europe/Moscow");
        var message = new Message(
                UUID.randomUUID(),
                conversationId,
                tenantId,
                "user-42",
                "request-1",
                RAW_TEXT,
                "ru-RU",
                "Europe/Moscow",
                Instant.parse("2026-09-14T10:00:00Z"),
                null);

        assertThat(command.toString()).doesNotContain(RAW_TEXT);
        assertThat(message.toString()).doesNotContain(RAW_TEXT);
    }
}
