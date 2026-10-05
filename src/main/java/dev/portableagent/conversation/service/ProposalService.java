package dev.portableagent.conversation.service;

import dev.portableagent.conversation.client.Proposal;
import dev.portableagent.conversation.model.Message;
import dev.portableagent.conversation.model.SavedReply;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProposalService {

    private final Map<String, ProposalHandler> handlers;

    public SavedReply make(Message message, String accessToken, Proposal proposal) {
        var handler = handlers.get(proposal.connector());
        if (handler == null) {
            throw new IllegalArgumentException("Unsupported connector: " + proposal.connector());
        }
        return handler.make(message, accessToken, proposal);
    }
}
