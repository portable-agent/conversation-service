package dev.portableagent.conversation.service;

import dev.portableagent.conversation.client.Proposal;
import dev.portableagent.conversation.model.Message;
import dev.portableagent.conversation.model.SavedReply;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FakeCalendarHandler implements ProposalHandler {

    private final ActionReplyService actions;

    @Override
    public String connector() {
        return "fake-calendar";
    }

    @Override
    public SavedReply make(Message message, String accessToken, Proposal proposal) {
        return actions.make(message, accessToken, proposal);
    }
}
