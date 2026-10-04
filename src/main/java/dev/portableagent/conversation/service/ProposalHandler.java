package dev.portableagent.conversation.service;

import dev.portableagent.conversation.client.Proposal;
import dev.portableagent.conversation.model.Message;
import dev.portableagent.conversation.model.SavedReply;

public interface ProposalHandler {

    String connector();

    SavedReply make(Message message, String accessToken, Proposal proposal);
}
