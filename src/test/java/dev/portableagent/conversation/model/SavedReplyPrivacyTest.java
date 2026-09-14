package dev.portableagent.conversation.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SavedReplyPrivacyTest {

    @Test
    void toString_shouldNotContainReplyData() {
        var reply = new SavedReply(ReplyType.TEXT, Map.of("text", "Личный ответ"));
        var decision = WorkDecision.started(UUID.fromString("40000000-0000-0000-0000-000000000001"));

        assertThat(reply.toString()).doesNotContain("Личный ответ");
        assertThat(decision.toString()).doesNotContain("40000000-0000-0000-0000-000000000001");
    }
}
