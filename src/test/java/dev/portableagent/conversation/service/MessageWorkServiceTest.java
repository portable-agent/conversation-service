package dev.portableagent.conversation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.portableagent.conversation.config.ConversationProperties;
import dev.portableagent.conversation.model.MessageWork;
import dev.portableagent.conversation.model.ReplyType;
import dev.portableagent.conversation.model.SavedReply;
import dev.portableagent.conversation.model.WorkError;
import dev.portableagent.conversation.model.WorkStart;
import dev.portableagent.conversation.model.WorkStatus;
import dev.portableagent.conversation.repository.MessageWorkRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageWorkServiceTest {

    private static final UUID MESSAGE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID WORK_TOKEN = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-09-14T10:00:00Z");
    private static final Duration WORK_TIMEOUT = Duration.ofMinutes(2);

    @Mock
    private MessageWorkRepository repository;

    private MessageWorkService service;

    @BeforeEach
    void setUp() {
        service = new MessageWorkService(
                repository,
                Clock.fixed(NOW, ZoneOffset.UTC),
                new ConversationProperties(Duration.ofHours(24), 100, WORK_TIMEOUT));
    }

    @Test
    void start_whenMessageIsNew_shouldClaimWork() {
        when(repository.findById(MESSAGE_ID)).thenReturn(Optional.of(work(WorkStatus.NEW, null, null)));
        when(repository.tryStart(MESSAGE_ID, NOW, NOW.minus(WORK_TIMEOUT))).thenReturn(Optional.of(WORK_TOKEN));

        var decision = service.start(MESSAGE_ID);

        assertThat(decision.status()).isEqualTo(WorkStart.STARTED);
        assertThat(decision.token()).isEqualTo(WORK_TOKEN);
    }

    @Test
    void start_whenWorkIsFresh_shouldReturnBusy() {
        when(repository.findById(MESSAGE_ID))
                .thenReturn(Optional.of(work(WorkStatus.PROCESSING, NOW.minusSeconds(30), null)));

        assertThat(service.start(MESSAGE_ID).status()).isEqualTo(WorkStart.BUSY);
        verify(repository, never()).tryStart(MESSAGE_ID, NOW, NOW.minus(WORK_TIMEOUT));
    }

    @Test
    void start_whenWorkerStopped_shouldClaimWorkAgain() {
        when(repository.findById(MESSAGE_ID))
                .thenReturn(Optional.of(work(WorkStatus.PROCESSING, NOW.minusSeconds(121), null)));
        when(repository.tryStart(MESSAGE_ID, NOW, NOW.minus(WORK_TIMEOUT))).thenReturn(Optional.of(WORK_TOKEN));

        assertThat(service.start(MESSAGE_ID).status()).isEqualTo(WorkStart.STARTED);
    }

    @Test
    void start_whenPreviousTryFailed_shouldClaimWorkAgain() {
        var failed =
                new MessageWork(MESSAGE_ID, WorkStatus.FAILED, 1, null, null, null, WorkError.AGENT_UNAVAILABLE, NOW);
        when(repository.findById(MESSAGE_ID)).thenReturn(Optional.of(failed));
        when(repository.tryStart(MESSAGE_ID, NOW, NOW.minus(WORK_TIMEOUT))).thenReturn(Optional.of(WORK_TOKEN));

        assertThat(service.start(MESSAGE_ID).status()).isEqualTo(WorkStart.STARTED);
    }

    @Test
    void start_whenResultIsReady_shouldReturnReadyWithoutClaim() {
        var reply = new SavedReply(ReplyType.TEXT, Map.of("text", "Уточните время"));
        when(repository.findById(MESSAGE_ID)).thenReturn(Optional.of(work(WorkStatus.READY, null, reply)));

        var decision = service.start(MESSAGE_ID);

        assertThat(decision.status()).isEqualTo(WorkStart.READY);
        assertThat(decision.reply()).isEqualTo(reply);
        verify(repository, never()).tryStart(MESSAGE_ID, NOW, NOW.minus(WORK_TIMEOUT));
    }

    @Test
    void start_whenResultWasErased_shouldNotStartAgain() {
        when(repository.findById(MESSAGE_ID)).thenReturn(Optional.of(work(WorkStatus.ERASED, null, null)));

        assertThat(service.start(MESSAGE_ID).status()).isEqualTo(WorkStart.ERASED);
        verify(repository, never()).tryStart(MESSAGE_ID, NOW, NOW.minus(WORK_TIMEOUT));
    }

    @Test
    void start_whenAnotherWorkerFinishesFirst_shouldReturnSavedState() {
        var reply = new SavedReply(ReplyType.TEXT, Map.of("text", "Уточните время"));
        when(repository.findById(MESSAGE_ID))
                .thenReturn(
                        Optional.of(work(WorkStatus.NEW, null, null)),
                        Optional.of(work(WorkStatus.READY, null, reply)));
        when(repository.tryStart(MESSAGE_ID, NOW, NOW.minus(WORK_TIMEOUT))).thenReturn(Optional.empty());

        assertThat(service.start(MESSAGE_ID).status()).isEqualTo(WorkStart.READY);
    }

    @Test
    void complete_whenWorkerHasResult_shouldSaveReadyReply() {
        var reply = new SavedReply(ReplyType.TEXT, Map.of("text", "Уточните время"));

        service.complete(MESSAGE_ID, WORK_TOKEN, reply);

        verify(repository).saveReady(MESSAGE_ID, WORK_TOKEN, reply, NOW);
    }

    @Test
    void fail_whenAgentIsUnavailable_shouldSaveOnlySafeCode() {
        service.fail(MESSAGE_ID, WORK_TOKEN, WorkError.AGENT_UNAVAILABLE);

        verify(repository).saveFailed(MESSAGE_ID, WORK_TOKEN, WorkError.AGENT_UNAVAILABLE, NOW);
    }

    private MessageWork work(WorkStatus status, Instant startedAt, SavedReply reply) {
        var token = status == WorkStatus.PROCESSING ? WORK_TOKEN : null;
        return new MessageWork(MESSAGE_ID, status, 1, startedAt, token, reply, null, NOW);
    }
}
