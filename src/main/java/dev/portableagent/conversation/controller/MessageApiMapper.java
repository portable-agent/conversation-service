package dev.portableagent.conversation.controller;

import dev.portableagent.conversation.api.model.ActionConfirmationWidget;
import dev.portableagent.conversation.api.model.ActionConfirmationWidgetActionsInner;
import dev.portableagent.conversation.api.model.ActionConfirmationWidgetFieldsInner;
import dev.portableagent.conversation.api.model.ConfirmationReply;
import dev.portableagent.conversation.api.model.MessageRequest;
import dev.portableagent.conversation.api.model.MessageResponse;
import dev.portableagent.conversation.api.model.MessageResponseReply;
import dev.portableagent.conversation.api.model.TextReply;
import dev.portableagent.conversation.model.ReplyType;
import dev.portableagent.conversation.model.SavedReply;
import dev.portableagent.conversation.service.HandleMessageCommand;
import dev.portableagent.conversation.service.MessageResult;
import dev.portableagent.conversation.service.StoreMessageCommand;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class MessageApiMapper {

    private MessageApiMapper() {}

    static HandleMessageCommand command(MessageRequest request, UUID tenantId, String subject, String token) {
        var context = request.getContext();
        return new HandleMessageCommand(
                new StoreMessageCommand(
                        tenantId,
                        subject,
                        request.getConversationId(),
                        request.getRequestKey(),
                        request.getText(),
                        context.getLocale(),
                        context.getTimeZone()),
                token);
    }

    static MessageResponse response(MessageResult result) {
        return new MessageResponse(result.messageId(), result.conversationId(), reply(result.reply()));
    }

    private static MessageResponseReply reply(SavedReply reply) {
        if (reply.type() == ReplyType.TEXT) {
            return new TextReply("text", text(reply.data(), "text"));
        }
        var data = reply.data();
        var card = new ActionConfirmationWidget(
                ActionConfirmationWidget.SchemaVersionEnum.fromValue(number(data, "schemaVersion")),
                ActionConfirmationWidget.WidgetEnum.fromValue(text(data, "widget")),
                UUID.fromString(text(data, "actionId")),
                text(data, "payloadHash"),
                text(data, "title"),
                fields(data),
                actions(data));
        return new ConfirmationReply("confirmation", card);
    }

    private static List<ActionConfirmationWidgetFieldsInner> fields(Map<String, Object> data) {
        return maps(data, "fields").stream()
                .map(field -> new ActionConfirmationWidgetFieldsInner(text(field, "label"), text(field, "value"))
                        .sensitive(Boolean.TRUE.equals(field.get("sensitive"))))
                .toList();
    }

    private static List<ActionConfirmationWidgetActionsInner> actions(Map<String, Object> data) {
        return maps(data, "actions").stream()
                .map(action -> new ActionConfirmationWidgetActionsInner(
                        ActionConfirmationWidgetActionsInner.IdEnum.fromValue(text(action, "id")),
                        text(action, "label")))
                .toList();
    }

    private static List<Map<String, Object>> maps(Map<String, Object> data, String name) {
        if (!(data.get(name) instanceof List<?> values)) {
            throw new IllegalArgumentException("Reply has no " + name);
        }
        return values.stream().map(MessageApiMapper::map).toList();
    }

    private static String text(Map<String, Object> data, String name) {
        if (!(data.get(name) instanceof String value) || value.isBlank()) {
            throw new IllegalArgumentException("Reply has no " + name);
        }
        return value;
    }

    private static int number(Map<String, Object> data, String name) {
        if (!(data.get(name) instanceof Number value)) {
            throw new IllegalArgumentException("Reply has no " + name);
        }
        return value.intValue();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("Reply list item is invalid");
        }
        return (Map<String, Object>) value;
    }
}
