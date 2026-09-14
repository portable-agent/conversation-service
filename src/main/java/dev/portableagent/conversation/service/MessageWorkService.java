package dev.portableagent.conversation.service;

import dev.portableagent.conversation.config.ConversationProperties;
import dev.portableagent.conversation.exception.MessageNotFound;
import dev.portableagent.conversation.model.MessageWork;
import dev.portableagent.conversation.model.SavedReply;
import dev.portableagent.conversation.model.WorkDecision;
import dev.portableagent.conversation.model.WorkError;
import dev.portableagent.conversation.model.WorkStart;
import dev.portableagent.conversation.repository.MessageWorkRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageWorkService {

    private final MessageWorkRepository repository;
    private final Clock clock;
    private final ConversationProperties properties;

    @Transactional
    public WorkDecision start(UUID messageId) {
        var now = clock.instant();
        var staleBefore = now.minus(properties.workTimeout());
        var work = find(messageId);

        var current = currentDecision(work, staleBefore);
        if (current.isPresent()) {
            return current.get();
        }
        var token = repository.tryStart(messageId, now, staleBefore);
        if (token.isPresent()) {
            return WorkDecision.started(token.get());
        }
        return currentDecision(find(messageId), staleBefore).orElse(WorkDecision.notStarted(WorkStart.BUSY));
    }

    @Transactional
    public void complete(UUID messageId, UUID token, SavedReply reply) {
        repository.saveReady(messageId, token, reply, clock.instant());
    }

    @Transactional
    public void fail(UUID messageId, UUID token, WorkError error) {
        repository.saveFailed(messageId, token, error, clock.instant());
    }

    private MessageWork find(UUID messageId) {
        return repository.findById(messageId).orElseThrow(() -> new MessageNotFound(messageId));
    }

    private Optional<WorkDecision> currentDecision(MessageWork work, Instant staleBefore) {
        return switch (work.status()) {
            case READY -> Optional.of(WorkDecision.ready(work.reply()));
            case ERASED -> Optional.of(WorkDecision.notStarted(WorkStart.ERASED));
            case PROCESSING ->
                work.startedAt().isAfter(staleBefore)
                        ? Optional.of(WorkDecision.notStarted(WorkStart.BUSY))
                        : Optional.empty();
            case NEW, FAILED -> Optional.empty();
        };
    }
}
