package dev.portableagent.conversation.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClientModelTest {

    @Test
    void agentReply_shouldContainExactlyOneResult() {
        var proposal = new Proposal("calendar.create_event", "calendar", Map.of());

        assertThatThrownBy(() -> new AgentReply(null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AgentReply(proposal, "question")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void models_shouldHidePersonalDataInLogs() {
        var privateValue = "private meeting";
        var proposal = new Proposal("calendar.create_event", "calendar", Map.of("title", privateValue));
        var action = new SavedAction(UUID.randomUUID(), "a".repeat(64), Map.of("title", privateValue));

        assertThat(proposal.toString()).doesNotContain(privateValue);
        assertThat(action.toString()).doesNotContain(privateValue);
        assertThat(AgentReply.question(privateValue).toString()).doesNotContain(privateValue);
    }

    @Test
    void savedAction_shouldUseContractHashFormat() {
        assertThatThrownBy(() -> new SavedAction(UUID.randomUUID(), "A".repeat(64), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
