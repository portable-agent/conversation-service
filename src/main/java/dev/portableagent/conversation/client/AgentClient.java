package dev.portableagent.conversation.client;

import dev.portableagent.conversation.model.Message;

public interface AgentClient {

    AgentReply ask(Message message, String accessToken);
}
