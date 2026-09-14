package dev.portableagent.conversation.repository;

import static dev.portableagent.conversation.db.tables.ConversationMessages.CONVERSATION_MESSAGES;

import dev.portableagent.conversation.model.Message;
import dev.portableagent.conversation.model.WorkStatus;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MessageRepository {

    private final DSLContext db;

    public Optional<Message> findByRequest(UUID tenantId, String subject, String requestKey) {
        return db.selectFrom(CONVERSATION_MESSAGES)
                .where(CONVERSATION_MESSAGES.TENANT_ID.eq(tenantId))
                .and(CONVERSATION_MESSAGES.SUBJECT.eq(subject))
                .and(CONVERSATION_MESSAGES.REQUEST_KEY.eq(requestKey))
                .fetchOptional(this::toMessage);
    }

    public boolean saveIfMissing(Message message) {
        int changed = db.insertInto(CONVERSATION_MESSAGES)
                .set(CONVERSATION_MESSAGES.ID, message.id())
                .set(CONVERSATION_MESSAGES.CONVERSATION_ID, message.conversationId())
                .set(CONVERSATION_MESSAGES.TENANT_ID, message.tenantId())
                .set(CONVERSATION_MESSAGES.SUBJECT, message.subject())
                .set(CONVERSATION_MESSAGES.REQUEST_KEY, message.requestKey())
                .set(CONVERSATION_MESSAGES.MESSAGE_TEXT, message.text())
                .set(CONVERSATION_MESSAGES.LOCALE, message.locale())
                .set(CONVERSATION_MESSAGES.TIME_ZONE, message.timeZone())
                .set(CONVERSATION_MESSAGES.CREATED_AT, utc(message.createdAt()))
                .set(CONVERSATION_MESSAGES.ERASED_AT, nullableUtc(message.erasedAt()))
                .set(CONVERSATION_MESSAGES.WORK_UPDATED_AT, utc(message.createdAt()))
                .onConflict(
                        CONVERSATION_MESSAGES.TENANT_ID,
                        CONVERSATION_MESSAGES.SUBJECT,
                        CONVERSATION_MESSAGES.REQUEST_KEY)
                .doNothing()
                .execute();
        return changed == 1;
    }

    public void eraseText(UUID conversationId, Instant now) {
        db.update(CONVERSATION_MESSAGES)
                .setNull(CONVERSATION_MESSAGES.MESSAGE_TEXT)
                .set(CONVERSATION_MESSAGES.ERASED_AT, utc(now))
                .set(CONVERSATION_MESSAGES.WORK_STATUS, WorkStatus.ERASED.name())
                .setNull(CONVERSATION_MESSAGES.WORK_STARTED_AT)
                .setNull(CONVERSATION_MESSAGES.WORK_TOKEN)
                .setNull(CONVERSATION_MESSAGES.REPLY_TYPE)
                .setNull(CONVERSATION_MESSAGES.REPLY_DATA)
                .setNull(CONVERSATION_MESSAGES.ERROR_CODE)
                .set(CONVERSATION_MESSAGES.WORK_UPDATED_AT, utc(now))
                .where(CONVERSATION_MESSAGES.CONVERSATION_ID.eq(conversationId))
                .execute();
    }

    private Message toMessage(Record row) {
        var erasedAt = row.get(CONVERSATION_MESSAGES.ERASED_AT);
        return new Message(
                row.get(CONVERSATION_MESSAGES.ID),
                row.get(CONVERSATION_MESSAGES.CONVERSATION_ID),
                row.get(CONVERSATION_MESSAGES.TENANT_ID),
                row.get(CONVERSATION_MESSAGES.SUBJECT),
                row.get(CONVERSATION_MESSAGES.REQUEST_KEY),
                row.get(CONVERSATION_MESSAGES.MESSAGE_TEXT),
                row.get(CONVERSATION_MESSAGES.LOCALE),
                row.get(CONVERSATION_MESSAGES.TIME_ZONE),
                row.get(CONVERSATION_MESSAGES.CREATED_AT).toInstant(),
                erasedAt == null ? null : erasedAt.toInstant());
    }

    private OffsetDateTime utc(Instant value) {
        return value.atOffset(ZoneOffset.UTC);
    }

    private OffsetDateTime nullableUtc(Instant value) {
        return value == null ? null : utc(value);
    }
}
