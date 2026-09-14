package dev.portableagent.conversation.repository;

import static dev.portableagent.conversation.db.tables.Conversations.CONVERSATIONS;
import static org.assertj.core.api.Assertions.assertThat;

import dev.portableagent.conversation.model.Conversation;
import dev.portableagent.conversation.model.ConversationStatus;
import dev.portableagent.conversation.model.Message;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class ConversationStorageTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    private static final Instant NOW = Instant.parse("2026-09-14T10:00:00Z");
    private static DSLContext db;
    private static Connection connection;

    @BeforeAll
    static void setUpDatabase() throws SQLException {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load()
                .migrate();
        connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        db = DSL.using(connection, SQLDialect.POSTGRES);
    }

    @AfterAll
    static void closeDatabase() throws SQLException {
        connection.close();
    }

    @Test
    void findOpen_whenOwnerMatchesAndTtlIsActive_returnsConversation() {
        var repository = new ConversationRepository(db);
        var conversation = conversation(NOW.plusSeconds(60));
        repository.create(conversation);

        assertThat(repository.findOpen(conversation.tenantId(), conversation.subject(), conversation.id(), NOW))
                .contains(conversation);
        assertThat(repository.findOpen(UUID.randomUUID(), conversation.subject(), conversation.id(), NOW))
                .isEmpty();
        assertThat(repository.findOpen(
                        conversation.tenantId(), conversation.subject(), conversation.id(), NOW.plusSeconds(60)))
                .isEmpty();
    }

    @Test
    void saveIfMissing_whenRequestKeyIsRepeated_keepsOneMessage() {
        var conversationRepository = new ConversationRepository(db);
        var messageRepository = new MessageRepository(db);
        var conversation = conversation(NOW.plusSeconds(60));
        conversationRepository.create(conversation);
        var first = message(conversation, "same-request", "Первый текст");
        var second = message(conversation, "same-request", "Другой текст");

        assertThat(messageRepository.saveIfMissing(first)).isTrue();
        assertThat(messageRepository.saveIfMissing(second)).isFalse();

        var saved = messageRepository
                .findByRequest(conversation.tenantId(), conversation.subject(), "same-request")
                .orElseThrow();
        assertThat(saved.id()).isEqualTo(first.id());
        assertThat(saved.text()).isEqualTo("Первый текст");
    }

    @Test
    void eraseText_whenConversationIsClosed_removesOnlyRawText() {
        var conversationRepository = new ConversationRepository(db);
        var messageRepository = new MessageRepository(db);
        var conversation = conversation(NOW.plusSeconds(60));
        conversationRepository.create(conversation);
        messageRepository.saveIfMissing(message(conversation, "erase-request", "Секретный текст"));

        messageRepository.eraseText(conversation.id(), NOW);
        conversationRepository.close(conversation.id(), NOW);

        var saved = messageRepository
                .findByRequest(conversation.tenantId(), conversation.subject(), "erase-request")
                .orElseThrow();
        assertThat(saved.text()).isNull();
        assertThat(saved.erasedAt()).isEqualTo(NOW);
        assertThat(repositoryStatus(conversation.id())).isEqualTo(ConversationStatus.CLOSED.name());
    }

    private Conversation conversation(Instant expiresAt) {
        return new Conversation(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "user-" + UUID.randomUUID(),
                ConversationStatus.OPEN,
                expiresAt,
                NOW,
                NOW);
    }

    private Message message(Conversation conversation, String requestKey, String text) {
        return new Message(
                UUID.randomUUID(),
                conversation.id(),
                conversation.tenantId(),
                conversation.subject(),
                requestKey,
                text,
                "ru-RU",
                "Europe/Moscow",
                NOW,
                null);
    }

    private String repositoryStatus(UUID conversationId) {
        return db.select(CONVERSATIONS.STATUS)
                .from(CONVERSATIONS)
                .where(CONVERSATIONS.ID.eq(conversationId))
                .fetchOne(CONVERSATIONS.STATUS);
    }
}
