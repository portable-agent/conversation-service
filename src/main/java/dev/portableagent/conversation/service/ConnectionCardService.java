package dev.portableagent.conversation.service;

import dev.portableagent.conversation.client.ConnectionClient;
import dev.portableagent.conversation.model.ReplyType;
import dev.portableagent.conversation.model.SavedReply;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConnectionCardService {

    private final ConnectionClient connections;

    public SavedReply saved(String provider) {
        return new SavedReply(
                ReplyType.CONNECTION,
                Map.of(
                        "schemaVersion", 1,
                        "widget", "connection",
                        "provider", provider,
                        "title", "Подключить Google Calendar",
                        "text", "Подключите календарь и повторите команду."));
    }

    public SavedReply show(SavedReply saved, String accessToken) {
        var provider = text(saved.data(), "provider");
        var url = connections.start(provider, accessToken);
        var data = new HashMap<>(saved.data());
        data.put("button", Map.of("label", "Подключить", "url", url.toString()));
        return new SavedReply(saved.type(), data);
    }

    private String text(Map<String, Object> data, String name) {
        if (!(data.get(name) instanceof String value) || value.isBlank()) {
            throw new IllegalArgumentException("Connection card has no " + name);
        }
        return value;
    }
}
