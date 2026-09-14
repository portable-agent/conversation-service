package dev.portableagent.conversation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.portableagent.conversation.model.Conversation;
import dev.portableagent.conversation.model.ConversationStatus;
import dev.portableagent.conversation.model.Message;
import dev.portableagent.conversation.repository.ConversationRepository;
import dev.portableagent.conversation.repository.MessageRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-14T10:00:00Z");
    private static final Duration OPEN_TTL = Duration.ofHours(24);
    private static final UUID TENANT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID CONVERSATION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final String SUBJECT = "user-42";
    private static final String REQUEST_KEY = "telegram:update-100";

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    private MessageService service;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new MessageService(conversationRepository, messageRepository, clock, OPEN_TTL);
    }

    @Test
    void store_whenRequestWasAlreadySaved_returnsSameMessage() {
        var saved = message(CONVERSATION_ID);
        var command = command(null);
        when(messageRepository.findByRequest(TENANT_ID, SUBJECT, REQUEST_KEY)).thenReturn(Optional.of(saved));

        var result = service.store(command);

        assertThat(result).isSameAs(saved);
        verify(conversationRepository, never()).create(org.mockito.ArgumentMatchers.any());
        verify(messageRepository, never()).saveIfMissing(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void store_withoutConversation_opensConversationForTwentyFourHours() {
        var command = command(null);
        when(messageRepository.findByRequest(TENANT_ID, SUBJECT, REQUEST_KEY)).thenReturn(Optional.empty());
        when(messageRepository.saveIfMissing(org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);

        var result = service.store(command);

        var savedConversation = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).create(savedConversation.capture());
        assertThat(savedConversation.getValue().status()).isEqualTo(ConversationStatus.OPEN);
        assertThat(savedConversation.getValue().expiresAt()).isEqualTo(NOW.plus(OPEN_TTL));
        assertThat(result.conversationId())
                .isEqualTo(savedConversation.getValue().id());
        assertThat(result.text()).isEqualTo("Переведи Коле 100 рублей");
    }

    @Test
    void store_inConversationOwnedByAnotherUser_isRejected() {
        var command = command(CONVERSATION_ID);
        when(messageRepository.findByRequest(TENANT_ID, SUBJECT, REQUEST_KEY)).thenReturn(Optional.empty());
        when(conversationRepository.findOpen(TENANT_ID, SUBJECT, CONVERSATION_ID, NOW))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.store(command))
                .isInstanceOf(ConversationNotOpen.class)
                .hasMessageContaining(CONVERSATION_ID.toString());

        verify(messageRepository, never()).saveIfMissing(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void store_whenSameNewRequestWinsInAnotherThread_removesEmptyConversation() {
        var saved = message(CONVERSATION_ID);
        var command = command(null);
        when(messageRepository.findByRequest(TENANT_ID, SUBJECT, REQUEST_KEY))
                .thenReturn(Optional.empty(), Optional.of(saved));
        when(messageRepository.saveIfMissing(org.mockito.ArgumentMatchers.any()))
                .thenReturn(false);

        var result = service.store(command);

        var opened = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).create(opened.capture());
        verify(conversationRepository).delete(opened.getValue().id());
        assertThat(result).isSameAs(saved);
    }

    @Test
    void close_erasesTextBeforeClosingConversation() {
        when(conversationRepository.findOpen(TENANT_ID, SUBJECT, CONVERSATION_ID, NOW))
                .thenReturn(Optional.of(conversation()));

        service.close(TENANT_ID, SUBJECT, CONVERSATION_ID);

        verify(messageRepository).eraseText(CONVERSATION_ID, NOW);
        verify(conversationRepository).close(CONVERSATION_ID, NOW);
    }

    @Test
    void closeExpired_erasesRawTextInSmallBatches() {
        var secondId = UUID.fromString("20000000-0000-0000-0000-000000000002");
        when(conversationRepository.findExpired(NOW, 100)).thenReturn(java.util.List.of(CONVERSATION_ID, secondId));

        var count = service.closeExpired();

        assertThat(count).isEqualTo(2);
        verify(messageRepository).eraseText(CONVERSATION_ID, NOW);
        verify(messageRepository).eraseText(secondId, NOW);
        verify(conversationRepository).close(CONVERSATION_ID, NOW);
        verify(conversationRepository).close(secondId, NOW);
    }

    private StoreMessageCommand command(UUID conversationId) {
        return new StoreMessageCommand(
                TENANT_ID, SUBJECT, conversationId, REQUEST_KEY, "Переведи Коле 100 рублей", "ru-RU", "Europe/Moscow");
    }

    private Conversation conversation() {
        return new Conversation(
                CONVERSATION_ID, TENANT_ID, SUBJECT, ConversationStatus.OPEN, NOW.plus(OPEN_TTL), NOW, NOW);
    }

    private Message message(UUID conversationId) {
        return new Message(
                UUID.fromString("30000000-0000-0000-0000-000000000001"),
                conversationId,
                TENANT_ID,
                SUBJECT,
                REQUEST_KEY,
                "Переведи Коле 100 рублей",
                "ru-RU",
                "Europe/Moscow",
                NOW,
                null);
    }
}
