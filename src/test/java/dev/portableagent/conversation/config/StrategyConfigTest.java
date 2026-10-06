package dev.portableagent.conversation.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.portableagent.conversation.client.Proposal;
import dev.portableagent.conversation.model.ReplyType;
import dev.portableagent.conversation.model.SavedReply;
import dev.portableagent.conversation.service.ProposalHandler;
import dev.portableagent.conversation.service.ProposalService;
import dev.portableagent.conversation.service.ReplyViewService;
import dev.portableagent.conversation.service.ReplyViewer;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class StrategyConfigTest {

    @Test
    void context_whenStrategiesHaveBusinessKeys_shouldWireServicesWithPreparedMaps() {
        var handler = mock(ProposalHandler.class);
        var viewer = mock(ReplyViewer.class);
        var saved = new SavedReply(ReplyType.CONNECTION, Map.of("provider", "google-calendar"));
        var shown = new SavedReply(ReplyType.CONNECTION, Map.of("provider", "google-calendar", "shown", true));
        var proposal = new Proposal("calendar.create_event", "google-calendar", Map.of());
        when(handler.connector()).thenReturn("google-calendar");
        when(handler.make(null, "token", proposal)).thenReturn(saved);
        when(viewer.types()).thenReturn(Set.of(ReplyType.CONNECTION));
        when(viewer.show(saved, "token")).thenReturn(shown);

        new ApplicationContextRunner()
                .withUserConfiguration(ProposalConfig.class, ReplyConfig.class)
                .withBean(ProposalHandler.class, () -> handler)
                .withBean(ReplyViewer.class, () -> viewer)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(ProposalService.class).make(null, "token", proposal))
                            .isEqualTo(saved);
                    assertThat(context.getBean(ReplyViewService.class).show(saved, "token"))
                            .isEqualTo(shown);
                });
    }
}
