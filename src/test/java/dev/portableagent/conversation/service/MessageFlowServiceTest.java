package dev.portableagent.conversation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.portableagent.conversation.client.ActionClient;
import dev.portableagent.conversation.client.ActionUnavailable;
import dev.portableagent.conversation.client.AgentClient;
import dev.portableagent.conversation.client.AgentReply;
import dev.portableagent.conversation.client.AgentUnavailable;
import dev.portableagent.conversation.client.Proposal;
import dev.portableagent.conversation.client.SavedAction;
import dev.portableagent.conversation.exception.MessageBusy;
import dev.portableagent.conversation.model.Message;
import dev.portableagent.conversation.model.ReplyType;
import dev.portableagent.conversation.model.SavedReply;
import dev.portableagent.conversation.model.WorkDecision;
import dev.portableagent.conversation.model.WorkError;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageFlowServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID CONVERSATION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID MESSAGE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID WORK_TOKEN = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID ACTION_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final String ACCESS_TOKEN = "safe-user-token";
    private static final String PAYLOAD_HASH = "a".repeat(64);

    @Mock
    private MessageService messageService;

    @Mock
    private MessageWorkService workService;

    @Mock
    private AgentClient agentClient;

    @Mock
    private ActionClient actionClient;

    private MessageFlowService service;

    @BeforeEach
    void setUp() {
        var cards = new CardService(List.of(new CalendarCardMaker()));
        service = new MessageFlowService(messageService, workService, agentClient, actionClient, cards);
    }

    @Test
    void handle_whenAgentReturnsProposal_shouldCreateActionAndConfirmation() {
        var command = command();
        var message = message();
        var proposal = new Proposal("calendar.create_event", "fake-calendar", payload());
        var action = new SavedAction(ACTION_ID, PAYLOAD_HASH, payload());
        when(messageService.store(command.storeCommand())).thenReturn(message);
        when(workService.start(MESSAGE_ID)).thenReturn(WorkDecision.started(WORK_TOKEN));
        when(agentClient.ask(message, ACCESS_TOKEN)).thenReturn(AgentReply.proposal(proposal));
        when(actionClient.create(proposal, message.requestKey(), ACCESS_TOKEN)).thenReturn(action);

        var result = service.handle(command);

        assertThat(result.messageId()).isEqualTo(MESSAGE_ID);
        assertThat(result.conversationId()).isEqualTo(CONVERSATION_ID);
        assertThat(result.reply().type()).isEqualTo(ReplyType.CONFIRMATION);
        assertThat(result.reply().data())
                .containsEntry("actionId", ACTION_ID.toString())
                .containsEntry("payloadHash", PAYLOAD_HASH)
                .containsEntry("widget", "action_confirmation");
        verify(workService).complete(MESSAGE_ID, WORK_TOKEN, result.reply());
    }

    @Test
    void handle_whenAgentNeedsDetails_shouldReturnTextWithoutAction() {
        var command = command();
        var message = message();
        when(messageService.store(command.storeCommand())).thenReturn(message);
        when(workService.start(MESSAGE_ID)).thenReturn(WorkDecision.started(WORK_TOKEN));
        when(agentClient.ask(message, ACCESS_TOKEN)).thenReturn(AgentReply.question("Когда начать встречу?"));

        var result = service.handle(command);

        assertThat(result.reply()).isEqualTo(new SavedReply(ReplyType.TEXT, Map.of("text", "Когда начать встречу?")));
        verify(actionClient, never())
                .create(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
        verify(workService).complete(MESSAGE_ID, WORK_TOKEN, result.reply());
    }

    @Test
    void handle_whenReplyWasAlreadySaved_shouldReturnItWithoutNetworkCalls() {
        var command = command();
        var message = message();
        var saved = new SavedReply(ReplyType.TEXT, Map.of("text", "Когда начать встречу?"));
        when(messageService.store(command.storeCommand())).thenReturn(message);
        when(workService.start(MESSAGE_ID)).thenReturn(WorkDecision.ready(saved));

        var result = service.handle(command);

        assertThat(result.reply()).isSameAs(saved);
        verify(agentClient, never()).ask(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(actionClient, never())
                .create(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void handle_whenWorkIsBusy_shouldNotCallDependencies() {
        var command = command();
        when(messageService.store(command.storeCommand())).thenReturn(message());
        when(workService.start(MESSAGE_ID))
                .thenReturn(WorkDecision.notStarted(dev.portableagent.conversation.model.WorkStart.BUSY));

        assertThatThrownBy(() -> service.handle(command)).isInstanceOf(MessageBusy.class);

        verify(agentClient, never()).ask(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void handle_whenMessageWasErased_shouldNotCallDependencies() {
        var command = command();
        when(messageService.store(command.storeCommand())).thenReturn(message());
        when(workService.start(MESSAGE_ID))
                .thenReturn(WorkDecision.notStarted(dev.portableagent.conversation.model.WorkStart.ERASED));

        assertThatThrownBy(() -> service.handle(command))
                .isInstanceOf(dev.portableagent.conversation.exception.MessageErased.class);

        verify(agentClient, never()).ask(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void handle_whenAgentIsUnavailable_shouldSaveSafeError() {
        var command = command();
        var message = message();
        when(messageService.store(command.storeCommand())).thenReturn(message);
        when(workService.start(MESSAGE_ID)).thenReturn(WorkDecision.started(WORK_TOKEN));
        when(agentClient.ask(message, ACCESS_TOKEN)).thenThrow(new AgentUnavailable());

        assertThatThrownBy(() -> service.handle(command)).isInstanceOf(AgentUnavailable.class);

        verify(workService).fail(MESSAGE_ID, WORK_TOKEN, WorkError.AGENT_UNAVAILABLE);
    }

    @Test
    void handle_whenActionIsUnavailable_shouldSaveSafeError() {
        var command = command();
        var message = message();
        var proposal = new Proposal("calendar.create_event", "fake-calendar", payload());
        when(messageService.store(command.storeCommand())).thenReturn(message);
        when(workService.start(MESSAGE_ID)).thenReturn(WorkDecision.started(WORK_TOKEN));
        when(agentClient.ask(message, ACCESS_TOKEN)).thenReturn(AgentReply.proposal(proposal));
        when(actionClient.create(proposal, message.requestKey(), ACCESS_TOKEN)).thenThrow(new ActionUnavailable());

        assertThatThrownBy(() -> service.handle(command)).isInstanceOf(ActionUnavailable.class);

        verify(workService).fail(MESSAGE_ID, WORK_TOKEN, WorkError.ACTION_UNAVAILABLE);
    }

    @Test
    void handle_whenReplyCannotBeBuilt_shouldSaveUnexpectedError() {
        var command = command();
        var message = message();
        var unknown = new Proposal("unknown.action", "fake", Map.of("value", "private"));
        var action = new SavedAction(ACTION_ID, PAYLOAD_HASH, Map.of("value", "private"));
        when(messageService.store(command.storeCommand())).thenReturn(message);
        when(workService.start(MESSAGE_ID)).thenReturn(WorkDecision.started(WORK_TOKEN));
        when(agentClient.ask(message, ACCESS_TOKEN)).thenReturn(AgentReply.proposal(unknown));
        when(actionClient.create(unknown, message.requestKey(), ACCESS_TOKEN)).thenReturn(action);

        assertThatThrownBy(() -> service.handle(command)).isInstanceOf(IllegalArgumentException.class);

        verify(workService).fail(MESSAGE_ID, WORK_TOKEN, WorkError.UNEXPECTED);
    }

    private HandleMessageCommand command() {
        var store = new StoreMessageCommand(
                TENANT_ID, "user-42", null, "telegram:update-100", "Создай встречу", "ru-RU", "Europe/Moscow");
        return new HandleMessageCommand(store, ACCESS_TOKEN);
    }

    private Message message() {
        return new Message(
                MESSAGE_ID,
                CONVERSATION_ID,
                TENANT_ID,
                "user-42",
                "telegram:update-100",
                "Создай встречу",
                "ru-RU",
                "Europe/Moscow",
                Instant.parse("2026-09-15T10:00:00Z"),
                null);
    }

    private Map<String, Object> payload() {
        return Map.of(
                "title", "Обсуждение проекта",
                "startAt", "2030-09-08T12:00:00+03:00",
                "endAt", "2030-09-08T12:30:00+03:00",
                "timeZone", "Europe/Moscow");
    }
}
