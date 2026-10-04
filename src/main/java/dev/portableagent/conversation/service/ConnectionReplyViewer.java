package dev.portableagent.conversation.service;

import dev.portableagent.conversation.model.ReplyType;
import dev.portableagent.conversation.model.SavedReply;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConnectionReplyViewer implements ReplyViewer {

    private final ConnectionCardService cards;

    @Override
    public Set<ReplyType> types() {
        return Set.of(ReplyType.CONNECTION);
    }

    @Override
    public SavedReply show(SavedReply saved, String accessToken) {
        return cards.show(saved, accessToken);
    }
}
