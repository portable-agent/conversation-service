package dev.portableagent.conversation.service;

import dev.portableagent.conversation.client.SavedAction;
import dev.portableagent.conversation.model.SavedReply;

public interface CardMaker {

    String kind();

    SavedReply make(SavedAction action);
}
