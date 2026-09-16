package dev.portableagent.conversation.client;

public class AgentUnavailable extends RuntimeException {

    public AgentUnavailable() {
        super("Agent service is unavailable");
    }
}
