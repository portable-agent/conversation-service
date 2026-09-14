package dev.portableagent.conversation.repository;

import static dev.portableagent.conversation.db.tables.Conversations.CONVERSATIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.portableagent.conversation.exception.MessageWorkChanged;
import dev.portableagent.conversation.model.Conversation;
import dev.portableagent.conversation.model.ConversationStatus;
import dev.portableagent.conversation.model.Message;
import dev.portableagent.conversation.model.ReplyType;
import dev.portableagent.conversation.model.SavedReply;
import dev.portableagent.conversation.model.WorkStatus;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
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
import tools.jackson.databind.json.JsonMapper;

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

    @Test
    void work_whenWorkerStops_shouldRecoverAndKeepReadyReply() {
        var conversationRepository = new ConversationRepository(db);
        var messageRepository = new MessageRepository(db);
        var workRepository = workRepository(db);
        var conversation = conversation(NOW.plusSeconds(600));
        var message = message(conversation, "work-request", "Создай встречу");
        conversationRepository.create(conversation);
        messageRepository.saveIfMissing(message);

        assertThat(workRepository.findById(message.id()).orElseThrow().status()).isEqualTo(WorkStatus.NEW);
        var firstToken = workRepository
                .tryStart(message.id(), NOW, NOW.minusSeconds(120))
                .orElseThrow();
        assertThat(workRepository.tryStart(message.id(), NOW.plusSeconds(30), NOW.minusSeconds(90)))
                .isEmpty();
        var secondToken = workRepository
                .tryStart(message.id(), NOW.plusSeconds(121), NOW.plusSeconds(1))
                .orElseThrow();

        var reply = new SavedReply(ReplyType.TEXT, Map.of("text", "Уточните время"));
        assertThatThrownBy(() -> workRepository.saveReady(message.id(), firstToken, reply, NOW.plusSeconds(122)))
                .isInstanceOf(MessageWorkChanged.class);
        workRepository.saveReady(message.id(), secondToken, reply, NOW.plusSeconds(122));
        var ready = workRepository.findById(message.id()).orElseThrow();
        assertThat(ready.status()).isEqualTo(WorkStatus.READY);
        assertThat(ready.attempts()).isEqualTo(2);
        assertThat(ready.reply()).isEqualTo(reply);

        messageRepository.eraseText(conversation.id(), NOW.plusSeconds(123));
        var erased = workRepository.findById(message.id()).orElseThrow();
        assertThat(erased.status()).isEqualTo(WorkStatus.ERASED);
        assertThat(erased.reply()).isNull();
    }

    @Test
    void tryStart_whenTwoWorkersRunTogether_shouldClaimOnce() throws Exception {
        var conversation = conversation(NOW.plusSeconds(600));
        var message = message(conversation, "parallel-work", "Создай встречу");
        new ConversationRepository(db).create(conversation);
        new MessageRepository(db).saveIfMissing(message);
        var start = new CountDownLatch(1);

        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> claimAfterStart(message.id(), start));
            var second = pool.submit(() -> claimAfterStart(message.id(), start));
            start.countDown();

            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
        }
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

    private boolean claimAfterStart(UUID messageId, CountDownLatch start) throws Exception {
        start.await();
        try (var taskConnection =
                DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            return workRepository(DSL.using(taskConnection, SQLDialect.POSTGRES))
                    .tryStart(messageId, NOW, NOW.minusSeconds(120))
                    .isPresent();
        }
    }

    private MessageWorkRepository workRepository(DSLContext context) {
        return new MessageWorkRepository(context, JsonMapper.builder().build());
    }
}
