package dev.portableagent.conversation.service;

import dev.portableagent.conversation.model.ReplyType;
import dev.portableagent.conversation.model.SavedReply;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PlainReplyViewer implements ReplyViewer {

    @Override
    public Set<ReplyType> types() {
        return Set.of(ReplyType.TEXT, ReplyType.CONFIRMATION);
    }

    @Override
    public SavedReply show(SavedReply saved, String accessToken) {
        return saved;
    }
}
