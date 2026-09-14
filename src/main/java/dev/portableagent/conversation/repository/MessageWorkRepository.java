package dev.portableagent.conversation.repository;

import static dev.portableagent.conversation.db.tables.ConversationMessages.CONVERSATION_MESSAGES;

import dev.portableagent.conversation.exception.MessageWorkChanged;
import dev.portableagent.conversation.model.MessageWork;
import dev.portableagent.conversation.model.ReplyType;
import dev.portableagent.conversation.model.SavedReply;
import dev.portableagent.conversation.model.WorkError;
import dev.portableagent.conversation.model.WorkStatus;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.Record;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Repository
@RequiredArgsConstructor
public class MessageWorkRepository {

    private final DSLContext db;
    private final JsonMapper jsonMapper;

    public Optional<MessageWork> findById(UUID messageId) {
        return db.selectFrom(CONVERSATION_MESSAGES)
                .where(CONVERSATION_MESSAGES.ID.eq(messageId))
                .fetchOptional(this::toWork);
    }

    public Optional<UUID> tryStart(UUID messageId, Instant now, Instant staleBefore) {
        var token = UUID.randomUUID();
        return db.update(CONVERSATION_MESSAGES)
                .set(CONVERSATION_MESSAGES.WORK_STATUS, WorkStatus.PROCESSING.name())
                .set(CONVERSATION_MESSAGES.WORK_STARTED_AT, utc(now))
                .set(CONVERSATION_MESSAGES.WORK_TOKEN, token)
                .set(CONVERSATION_MESSAGES.WORK_ATTEMPTS, CONVERSATION_MESSAGES.WORK_ATTEMPTS.plus(1))
                .setNull(CONVERSATION_MESSAGES.ERROR_CODE)
                .set(CONVERSATION_MESSAGES.WORK_UPDATED_AT, utc(now))
                .where(CONVERSATION_MESSAGES.ID.eq(messageId))
                .and(CONVERSATION_MESSAGES
                        .WORK_STATUS
                        .in(WorkStatus.NEW.name(), WorkStatus.FAILED.name())
                        .or(CONVERSATION_MESSAGES
                                .WORK_STATUS
                                .eq(WorkStatus.PROCESSING.name())
                                .and(CONVERSATION_MESSAGES.WORK_STARTED_AT.le(utc(staleBefore)))))
                .returning(CONVERSATION_MESSAGES.WORK_TOKEN)
                .fetchOptional(CONVERSATION_MESSAGES.WORK_TOKEN);
    }

    public void saveReady(UUID messageId, UUID token, SavedReply reply, Instant now) {
        int changed = db.update(CONVERSATION_MESSAGES)
                .set(CONVERSATION_MESSAGES.WORK_STATUS, WorkStatus.READY.name())
                .setNull(CONVERSATION_MESSAGES.WORK_STARTED_AT)
                .setNull(CONVERSATION_MESSAGES.WORK_TOKEN)
                .set(CONVERSATION_MESSAGES.REPLY_TYPE, reply.type().name())
                .set(CONVERSATION_MESSAGES.REPLY_DATA, toJson(reply.data()))
                .setNull(CONVERSATION_MESSAGES.ERROR_CODE)
                .set(CONVERSATION_MESSAGES.WORK_UPDATED_AT, utc(now))
                .where(CONVERSATION_MESSAGES.ID.eq(messageId))
                .and(CONVERSATION_MESSAGES.WORK_STATUS.eq(WorkStatus.PROCESSING.name()))
                .and(CONVERSATION_MESSAGES.WORK_TOKEN.eq(token))
                .execute();
        checkChanged(messageId, changed);
    }

    public void saveFailed(UUID messageId, UUID token, WorkError error, Instant now) {
        int changed = db.update(CONVERSATION_MESSAGES)
                .set(CONVERSATION_MESSAGES.WORK_STATUS, WorkStatus.FAILED.name())
                .setNull(CONVERSATION_MESSAGES.WORK_STARTED_AT)
                .setNull(CONVERSATION_MESSAGES.WORK_TOKEN)
                .setNull(CONVERSATION_MESSAGES.REPLY_TYPE)
                .setNull(CONVERSATION_MESSAGES.REPLY_DATA)
                .set(CONVERSATION_MESSAGES.ERROR_CODE, error.name())
                .set(CONVERSATION_MESSAGES.WORK_UPDATED_AT, utc(now))
                .where(CONVERSATION_MESSAGES.ID.eq(messageId))
                .and(CONVERSATION_MESSAGES.WORK_STATUS.eq(WorkStatus.PROCESSING.name()))
                .and(CONVERSATION_MESSAGES.WORK_TOKEN.eq(token))
                .execute();
        checkChanged(messageId, changed);
    }

    private MessageWork toWork(Record row) {
        var startedAt = row.get(CONVERSATION_MESSAGES.WORK_STARTED_AT);
        var replyType = row.get(CONVERSATION_MESSAGES.REPLY_TYPE);
        var replyData = row.get(CONVERSATION_MESSAGES.REPLY_DATA);
        var errorCode = row.get(CONVERSATION_MESSAGES.ERROR_CODE);
        return new MessageWork(
                row.get(CONVERSATION_MESSAGES.ID),
                WorkStatus.valueOf(row.get(CONVERSATION_MESSAGES.WORK_STATUS)),
                row.get(CONVERSATION_MESSAGES.WORK_ATTEMPTS),
                startedAt == null ? null : startedAt.toInstant(),
                row.get(CONVERSATION_MESSAGES.WORK_TOKEN),
                replyType == null || replyData == null
                        ? null
                        : new SavedReply(ReplyType.valueOf(replyType), fromJson(replyData)),
                errorCode == null ? null : WorkError.valueOf(errorCode),
                row.get(CONVERSATION_MESSAGES.WORK_UPDATED_AT).toInstant());
    }

    private void checkChanged(UUID messageId, int changed) {
        if (changed != 1) {
            throw new MessageWorkChanged(messageId);
        }
    }

    private JSON toJson(Map<String, Object> data) {
        try {
            return JSON.valueOf(jsonMapper.writeValueAsString(data));
        } catch (JacksonException error) {
            throw new IllegalArgumentException("Reply is not valid JSON", error);
        }
    }

    private Map<String, Object> fromJson(JSON data) {
        try {
            return jsonMapper.readValue(data.data(), new TypeReference<>() {});
        } catch (JacksonException error) {
            throw new IllegalStateException("Saved reply is not valid JSON", error);
        }
    }

    private OffsetDateTime utc(Instant value) {
        return value.atOffset(ZoneOffset.UTC);
    }
}
