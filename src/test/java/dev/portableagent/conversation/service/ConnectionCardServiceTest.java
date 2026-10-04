package dev.portableagent.conversation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import dev.portableagent.conversation.client.ConnectionClient;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConnectionCardServiceTest {

    @Mock
    private ConnectionClient connections;

    @Test
    void show_shouldAddFreshUrlWithoutChangingStoredReply() {
        var service = new ConnectionCardService(connections);
        var saved = service.saved("google-calendar");
        when(connections.start("google-calendar", "user-token"))
                .thenReturn(URI.create("https://accounts.google.com/new-state"));

        var shown = service.show(saved, "user-token");

        assertThat(saved.data()).doesNotContainKey("button");
        assertThat(shown.data()).containsEntry("provider", "google-calendar");
        assertThat(shown.data().get("button"))
                .isEqualTo(Map.of("label", "Подключить", "url", "https://accounts.google.com/new-state"));
    }
}
