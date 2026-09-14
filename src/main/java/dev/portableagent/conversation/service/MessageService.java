package dev.portableagent.conversation.service;

import dev.portableagent.conversation.config.ConversationProperties;
import dev.portableagent.conversation.model.Conversation;
import dev.portableagent.conversation.model.ConversationStatus;
import dev.portableagent.conversation.model.Message;
import dev.portableagent.conversation.repository.ConversationRepository;
import dev.portableagent.conversation.repository.MessageRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final Clock clock;
    private final Duration openTtl;
    private final int cleanupBatchSize;

    public MessageService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            Clock clock,
            ConversationProperties properties) {
        this(conversationRepository, messageRepository, clock, properties.openTtl(), properties.cleanupBatchSize());
    }

    MessageService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            Clock clock,
            Duration openTtl) {
        this(conversationRepository, messageRepository, clock, openTtl, 100);
    }

    private MessageService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            Clock clock,
            Duration openTtl,
            int cleanupBatchSize) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.clock = clock;
        this.openTtl = openTtl;
        this.cleanupBatchSize = cleanupBatchSize;
    }

    @Transactional
    public Message store(StoreMessageCommand command) {
        var saved = messageRepository.findByRequest(command.tenantId(), command.subject(), command.requestKey());
        if (saved.isPresent()) {
            return saved.get();
        }

        var now = clock.instant();
        boolean newConversation = command.conversationId() == null;
        var conversation = newConversation ? openConversation(command, now) : findOpen(command, now);
        var message = new Message(
                UUID.randomUUID(),
                conversation.id(),
                command.tenantId(),
                command.subject(),
                command.requestKey(),
                command.text(),
                command.locale(),
                command.timeZone(),
                now,
                null);

        if (messageRepository.saveIfMissing(message)) {
            return message;
        }

        if (newConversation) {
            conversationRepository.delete(conversation.id());
        }
        return messageRepository
                .findByRequest(command.tenantId(), command.subject(), command.requestKey())
                .orElseThrow(() -> new IllegalStateException("Saved message cannot be found"));
    }

    @Transactional
    public void close(UUID tenantId, String subject, UUID conversationId) {
        var now = clock.instant();
        conversationRepository
                .findOpen(tenantId, subject, conversationId, now)
                .orElseThrow(() -> new ConversationNotOpen(conversationId));
        messageRepository.eraseText(conversationId, now);
        conversationRepository.close(conversationId, now);
    }

    @Transactional
    public int closeExpired() {
        var now = clock.instant();
        var expiredIds = conversationRepository.findExpired(now, cleanupBatchSize);
        expiredIds.forEach(conversationId -> {
            messageRepository.eraseText(conversationId, now);
            conversationRepository.close(conversationId, now);
        });
        return expiredIds.size();
    }

    private Conversation openConversation(StoreMessageCommand command, Instant now) {
        var conversation = new Conversation(
                UUID.randomUUID(),
                command.tenantId(),
                command.subject(),
                ConversationStatus.OPEN,
                now.plus(openTtl),
                now,
                now);
        conversationRepository.create(conversation);
        return conversation;
    }

    private Conversation findOpen(StoreMessageCommand command, Instant now) {
        return conversationRepository
                .findOpen(command.tenantId(), command.subject(), command.conversationId(), now)
                .orElseThrow(() -> new ConversationNotOpen(command.conversationId()));
    }
}
