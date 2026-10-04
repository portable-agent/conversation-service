package dev.portableagent.conversation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.portableagent.conversation.client.ConnectionClient;
import dev.portableagent.conversation.client.ConnectionStatus;
import dev.portableagent.conversation.client.Proposal;
import dev.portableagent.conversation.model.Message;
import dev.portableagent.conversation.model.ReplyType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarHandlerTest {

    @Mock
    private ConnectionClient connections;

    @Mock
    private ActionReplyService actions;

    @Test
    void make_whenConnectionIsMissing_shouldReturnSafeStoredCard() {
        var handler = new GoogleCalendarHandler(connections, actions, new ConnectionCardService(connections));
        when(connections.status("google-calendar", "user-token")).thenReturn(ConnectionStatus.MISSING);

        var reply = handler.make(message(), "user-token", proposal());

        assertThat(reply.type()).isEqualTo(ReplyType.CONNECTION);
        assertThat(reply.data())
                .containsEntry("provider", "google-calendar")
                .doesNotContainKeys("button", "url", "state");
        verify(actions, never())
                .make(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void make_whenOneConnectionIsReady_shouldCreateAction() {
        var cards = new ConnectionCardService(connections);
        var handler = new GoogleCalendarHandler(connections, actions, cards);
        var expected = cards.saved("google-calendar");
        var message = message();
        var proposal = proposal();
        when(connections.status("google-calendar", "user-token")).thenReturn(ConnectionStatus.READY);
        when(actions.make(message, "user-token", proposal)).thenReturn(expected);

        assertThat(handler.make(message, "user-token", proposal)).isSameAs(expected);
    }

    @Test
    void make_whenSeveralConnectionsExist_shouldNotChooseOne() {
        var handler = new GoogleCalendarHandler(connections, actions, new ConnectionCardService(connections));
        when(connections.status("google-calendar", "user-token")).thenReturn(ConnectionStatus.AMBIGUOUS);

        var reply = handler.make(message(), "user-token", proposal());

        assertThat(reply.type()).isEqualTo(ReplyType.TEXT);
        assertThat(reply.data().get("text"))
                .isEqualTo("Найдено несколько календарей. Оставьте одно активное подключение.");
        verify(actions, never())
                .make(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    private Message message() {
        return new Message(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "user-42",
                "telegram:1",
                "Создай встречу",
                "ru-RU",
                "Europe/Moscow",
                Instant.parse("2026-10-04T10:00:00Z"),
                null);
    }

    private Proposal proposal() {
        return new Proposal("calendar.create_event", "google-calendar", Map.of("title", "Встреча"));
    }
}
