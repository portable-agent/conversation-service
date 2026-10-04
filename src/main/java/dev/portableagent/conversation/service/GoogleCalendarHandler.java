package dev.portableagent.conversation.service;

import dev.portableagent.conversation.client.ConnectionClient;
import dev.portableagent.conversation.client.Proposal;
import dev.portableagent.conversation.model.Message;
import dev.portableagent.conversation.model.ReplyType;
import dev.portableagent.conversation.model.SavedReply;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoogleCalendarHandler implements ProposalHandler {

    private static final String GOOGLE_CALENDAR = "google-calendar";

    private final ConnectionClient connections;
    private final ActionReplyService actions;
    private final ConnectionCardService cards;

    @Override
    public String connector() {
        return GOOGLE_CALENDAR;
    }

    @Override
    public SavedReply make(Message message, String accessToken, Proposal proposal) {
        return switch (connections.status(GOOGLE_CALENDAR, accessToken)) {
            case READY -> actions.make(message, accessToken, proposal);
            case MISSING -> cards.saved(GOOGLE_CALENDAR);
            case AMBIGUOUS ->
                new SavedReply(
                        ReplyType.TEXT,
                        Map.of("text", "Найдено несколько календарей. Оставьте одно активное подключение."));
        };
    }
}
