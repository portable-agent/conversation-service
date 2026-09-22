package dev.portableagent.conversation.service;

import dev.portableagent.conversation.client.SavedAction;
import dev.portableagent.conversation.model.ReplyType;
import dev.portableagent.conversation.model.SavedReply;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CalendarCardMaker implements CardMaker {

    private static final String KIND = "calendar.create_event";

    @Override
    public String kind() {
        return KIND;
    }

    @Override
    public SavedReply make(SavedAction action) {
        var data = new LinkedHashMap<String, Object>();
        data.put("schemaVersion", 1);
        data.put("widget", "action_confirmation");
        data.put("actionId", action.id().toString());
        data.put("payloadHash", action.payloadHash());
        data.put("title", "Создать встречу");
        data.put("fields", fields(action.payload()));
        data.put(
                "actions",
                List.of(Map.of("id", "confirm", "label", "Подтвердить"), Map.of("id", "cancel", "label", "Отменить")));
        return new SavedReply(ReplyType.CONFIRMATION, data);
    }

    private List<Map<String, Object>> fields(Map<String, Object> payload) {
        var fields = new ArrayList<Map<String, Object>>();
        fields.add(field("Название", required(payload, "title"), false));
        fields.add(field("Начало", required(payload, "startAt"), false));
        fields.add(field("Конец", required(payload, "endAt"), false));
        fields.add(field("Часовой пояс", required(payload, "timeZone"), false));
        optionalField(fields, payload, "description", "Описание", true);
        optionalField(fields, payload, "attendees", "Участники", true);
        return List.copyOf(fields);
    }

    private Map<String, Object> field(String label, Object value, boolean sensitive) {
        var shownValue = shownValue(value);
        if (shownValue.isBlank()) {
            throw new IllegalArgumentException("Card field must not be blank: " + label);
        }
        return Map.of("label", label, "value", shownValue, "sensitive", sensitive);
    }

    private Object required(Map<String, Object> payload, String name) {
        var value = payload.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Saved action has no " + name);
        }
        return value;
    }

    private void optionalField(
            List<Map<String, Object>> fields,
            Map<String, Object> payload,
            String name,
            String label,
            boolean sensitive) {
        var value = payload.get(name);
        if (value != null) {
            fields.add(field(label, value, sensitive));
        }
    }

    private String shownValue(Object value) {
        if (value instanceof Collection<?> values) {
            return values.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(", "));
        }
        return String.valueOf(value);
    }
}
