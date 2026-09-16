package dev.portableagent.conversation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.portableagent.conversation.client.Proposal;
import dev.portableagent.conversation.client.SavedAction;
import dev.portableagent.conversation.model.ReplyType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CardServiceTest {

    @Test
    void make_whenCalendarAction_shouldShowSavedPayload() {
        var payload = Map.<String, Object>of(
                "title", "Обсуждение проекта",
                "startAt", "2030-09-08T12:00:00+03:00",
                "endAt", "2030-09-08T12:30:00+03:00",
                "timeZone", "Europe/Moscow");
        var proposal = new Proposal("calendar.create_event", "fake-calendar", payload);
        var action = new SavedAction(UUID.randomUUID(), "a".repeat(64), payload);
        var service = new CardService(List.of(new CalendarCardMaker()));

        var reply = service.make(proposal, action);

        assertThat(reply.type()).isEqualTo(ReplyType.CONFIRMATION);
        assertThat((List<?>) reply.data().get("fields")).hasSize(4);
        assertThat(reply.toString()).doesNotContain("Обсуждение проекта");
    }

    @Test
    void make_whenKindIsUnknown_shouldRejectIt() {
        var service = new CardService(List.of(new CalendarCardMaker()));
        var proposal = new Proposal("money.transfer", "fake-wallet", Map.of("amount", "100"));
        var action = new SavedAction(UUID.randomUUID(), "a".repeat(64), Map.of("amount", "100"));

        assertThatThrownBy(() -> service.make(proposal, action))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("money.transfer");
    }

    @Test
    void make_whenAttendeesArePresent_shouldMakeSchemaCompatibleString() {
        var payload = Map.<String, Object>of(
                "title", "Обсуждение проекта",
                "startAt", "2030-09-08T12:00:00+03:00",
                "endAt", "2030-09-08T12:30:00+03:00",
                "timeZone", "Europe/Moscow",
                "attendees", List.of("one@example.com", "two@example.com"));
        var proposal = new Proposal("calendar.create_event", "fake-calendar", payload);
        var action = new SavedAction(UUID.randomUUID(), "a".repeat(64), payload);
        var service = new CardService(List.of(new CalendarCardMaker()));

        var reply = service.make(proposal, action);

        @SuppressWarnings("unchecked")
        var fields = (List<Map<String, Object>>) reply.data().get("fields");
        assertThat(fields.getLast()).containsEntry("value", "one@example.com, two@example.com");
    }
}
