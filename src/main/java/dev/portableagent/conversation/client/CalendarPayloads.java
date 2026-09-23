package dev.portableagent.conversation.client;

import java.util.LinkedHashMap;
import java.util.Map;

final class CalendarPayloads {

    private CalendarPayloads() {}

    static Map<String, Object> fromAgent(
            dev.portableagent.conversation.agent.api.model.CalendarCreateEventPayload source) {
        var payload = base(source.getTitle(), source.getStartAt(), source.getEndAt(), source.getTimeZone());
        addOptional(payload, "description", source.getDescription());
        addOptional(payload, "attendees", source.getAttendees());
        return Map.copyOf(payload);
    }

    static dev.portableagent.conversation.action.api.model.CalendarCreateEventPayload toAction(
            Map<String, Object> source) {
        var payload = new dev.portableagent.conversation.action.api.model.CalendarCreateEventPayload(
                text(source, "title"), text(source, "startAt"), text(source, "endAt"), text(source, "timeZone"));
        if (source.get("description") instanceof String description) {
            payload.setDescription(description);
        }
        if (source.get("attendees") instanceof java.util.Collection<?> attendees) {
            payload.setAttendees(attendees.stream().map(String::valueOf).collect(java.util.stream.Collectors.toSet()));
        }
        return payload;
    }

    static Map<String, Object> fromAction(
            dev.portableagent.conversation.action.api.model.CalendarCreateEventPayload source) {
        var payload = base(source.getTitle(), source.getStartAt(), source.getEndAt(), source.getTimeZone());
        addOptional(payload, "description", source.getDescription());
        addOptional(payload, "attendees", source.getAttendees());
        return Map.copyOf(payload);
    }

    private static LinkedHashMap<String, Object> base(String title, String startAt, String endAt, String timeZone) {
        if (title == null
                || title.isBlank()
                || startAt == null
                || endAt == null
                || timeZone == null
                || timeZone.isBlank()) {
            throw new IllegalArgumentException("Calendar payload is incomplete");
        }
        var payload = new LinkedHashMap<String, Object>();
        payload.put("title", title);
        payload.put("startAt", startAt.toString());
        payload.put("endAt", endAt.toString());
        payload.put("timeZone", timeZone);
        return payload;
    }

    private static void addOptional(Map<String, Object> target, String name, Object value) {
        if (value != null) {
            target.put(name, value);
        }
    }

    private static String text(Map<String, Object> source, String name) {
        var value = source.get(name);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("Calendar payload has no " + name);
        }
        return text;
    }
}
