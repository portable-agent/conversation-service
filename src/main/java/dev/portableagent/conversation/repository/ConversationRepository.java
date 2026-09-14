package dev.portableagent.conversation.repository;

import static dev.portableagent.conversation.db.tables.Conversations.CONVERSATIONS;

import dev.portableagent.conversation.model.Conversation;
import dev.portableagent.conversation.model.ConversationStatus;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ConversationRepository {

    private final DSLContext db;

    public void create(Conversation conversation) {
        db.insertInto(CONVERSATIONS)
                .set(CONVERSATIONS.ID, conversation.id())
                .set(CONVERSATIONS.TENANT_ID, conversation.tenantId())
                .set(CONVERSATIONS.SUBJECT, conversation.subject())
                .set(CONVERSATIONS.STATUS, conversation.status().name())
                .set(CONVERSATIONS.EXPIRES_AT, utc(conversation.expiresAt()))
                .set(CONVERSATIONS.CREATED_AT, utc(conversation.createdAt()))
                .set(CONVERSATIONS.UPDATED_AT, utc(conversation.updatedAt()))
                .execute();
    }

    public Optional<Conversation> findOpen(UUID tenantId, String subject, UUID conversationId, Instant now) {
        return db.selectFrom(CONVERSATIONS)
                .where(CONVERSATIONS.ID.eq(conversationId))
                .and(CONVERSATIONS.TENANT_ID.eq(tenantId))
                .and(CONVERSATIONS.SUBJECT.eq(subject))
                .and(CONVERSATIONS.STATUS.eq(ConversationStatus.OPEN.name()))
                .and(CONVERSATIONS.EXPIRES_AT.gt(utc(now)))
                .fetchOptional(this::toConversation);
    }

    public void close(UUID conversationId, Instant now) {
        db.update(CONVERSATIONS)
                .set(CONVERSATIONS.STATUS, ConversationStatus.CLOSED.name())
                .set(CONVERSATIONS.UPDATED_AT, utc(now))
                .where(CONVERSATIONS.ID.eq(conversationId))
                .execute();
    }

    public List<UUID> findExpired(Instant now, int limit) {
        return db.select(CONVERSATIONS.ID)
                .from(CONVERSATIONS)
                .where(CONVERSATIONS.STATUS.eq(ConversationStatus.OPEN.name()))
                .and(CONVERSATIONS.EXPIRES_AT.le(utc(now)))
                .orderBy(CONVERSATIONS.EXPIRES_AT)
                .limit(limit)
                .fetch(CONVERSATIONS.ID);
    }

    public void delete(UUID conversationId) {
        db.deleteFrom(CONVERSATIONS).where(CONVERSATIONS.ID.eq(conversationId)).execute();
    }

    private Conversation toConversation(Record row) {
        return new Conversation(
                row.get(CONVERSATIONS.ID),
                row.get(CONVERSATIONS.TENANT_ID),
                row.get(CONVERSATIONS.SUBJECT),
                ConversationStatus.valueOf(row.get(CONVERSATIONS.STATUS)),
                row.get(CONVERSATIONS.EXPIRES_AT).toInstant(),
                row.get(CONVERSATIONS.CREATED_AT).toInstant(),
                row.get(CONVERSATIONS.UPDATED_AT).toInstant());
    }

    private OffsetDateTime utc(Instant value) {
        return value.atOffset(ZoneOffset.UTC);
    }
}
