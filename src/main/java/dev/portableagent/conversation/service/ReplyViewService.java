package dev.portableagent.conversation.service;

import dev.portableagent.conversation.model.ReplyType;
import dev.portableagent.conversation.model.SavedReply;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReplyViewService {

    private final Map<ReplyType, ReplyViewer> viewers;

    public SavedReply show(SavedReply saved, String accessToken) {
        var viewer = viewers.get(saved.type());
        if (viewer == null) {
            throw new IllegalArgumentException("Unsupported reply type: " + saved.type());
        }
        return viewer.show(saved, accessToken);
    }
}
