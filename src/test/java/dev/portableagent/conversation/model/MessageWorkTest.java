package dev.portableagent.conversation.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MessageWorkTest {

    private static final UUID MESSAGE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-09-14T10:00:00Z");

    @Test
    void create_whenProcessingHasNoStartTime_shouldRejectState() {
        assertThatThrownBy(() -> new MessageWork(MESSAGE_ID, WorkStatus.PROCESSING, 1, null, null, null, null, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PROCESSING work must have startedAt and token");
    }

    @Test
    void create_whenReadyHasNoReply_shouldRejectState() {
        assertThatThrownBy(() -> new MessageWork(MESSAGE_ID, WorkStatus.READY, 1, null, null, null, null, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("READY work must have reply");
    }
}
