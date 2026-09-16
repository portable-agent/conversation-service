package dev.portableagent.conversation.client;

public interface ActionClient {

    SavedAction create(Proposal proposal, String requestKey, String accessToken);
}
