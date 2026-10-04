package dev.portableagent.conversation.service;

import dev.portableagent.conversation.model.ReplyType;
import dev.portableagent.conversation.model.SavedReply;
import java.util.Set;

public interface ReplyViewer {

    Set<ReplyType> types();

    SavedReply show(SavedReply saved, String accessToken);
}
