package dev.portableagent.conversation.service;

import dev.portableagent.conversation.client.ActionClient;
import dev.portableagent.conversation.client.Proposal;
import dev.portableagent.conversation.model.Message;
import dev.portableagent.conversation.model.SavedReply;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActionReplyService {

    private final ActionClient actions;
    private final CardService cards;

    public SavedReply make(Message message, String accessToken, Proposal proposal) {
        var action = actions.create(proposal, message.requestKey(), accessToken);
        return cards.make(proposal, action);
    }
}
