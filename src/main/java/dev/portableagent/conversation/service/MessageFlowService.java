package dev.portableagent.conversation.service;

import dev.portableagent.conversation.client.ActionClient;
import dev.portableagent.conversation.client.ActionUnavailable;
import dev.portableagent.conversation.client.AgentClient;
import dev.portableagent.conversation.client.AgentReply;
import dev.portableagent.conversation.client.AgentUnavailable;
import dev.portableagent.conversation.exception.MessageBusy;
import dev.portableagent.conversation.exception.MessageErased;
import dev.portableagent.conversation.model.Message;
import dev.portableagent.conversation.model.ReplyType;
import dev.portableagent.conversation.model.SavedReply;
import dev.portableagent.conversation.model.WorkError;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MessageFlowService {

    private final MessageService messageService;
    private final MessageWorkService workService;
    private final AgentClient agentClient;
    private final ActionClient actionClient;
    private final CardService cardService;

    public MessageResult handle(HandleMessageCommand command) {
        var message = messageService.store(command.storeCommand());
        var work = workService.start(message.id());

        return switch (work.status()) {
            case READY -> result(message, work.reply());
            case BUSY -> throw new MessageBusy(message.id());
            case ERASED -> throw new MessageErased(message.id());
            case STARTED -> process(message, command.accessToken(), work.token());
        };
    }

    private MessageResult process(Message message, String accessToken, java.util.UUID workToken) {
        var reply = makeReply(message, accessToken, workToken);
        workService.complete(message.id(), workToken, reply);
        return result(message, reply);
    }

    private SavedReply makeReply(Message message, String accessToken, java.util.UUID workToken) {
        try {
            var agentReply = agentClient.ask(message, accessToken);
            return agentReply.needsDetails() ? textReply(agentReply) : actionReply(message, accessToken, agentReply);
        } catch (AgentUnavailable exception) {
            workService.fail(message.id(), workToken, WorkError.AGENT_UNAVAILABLE);
            throw exception;
        } catch (ActionUnavailable exception) {
            workService.fail(message.id(), workToken, WorkError.ACTION_UNAVAILABLE);
            throw exception;
        } catch (RuntimeException exception) {
            workService.fail(message.id(), workToken, WorkError.UNEXPECTED);
            throw exception;
        }
    }

    private SavedReply textReply(AgentReply reply) {
        return new SavedReply(ReplyType.TEXT, java.util.Map.of("text", reply.question()));
    }

    private SavedReply actionReply(Message message, String accessToken, AgentReply reply) {
        var action = actionClient.create(reply.proposal(), message.requestKey(), accessToken);
        return cardService.make(reply.proposal(), action);
    }

    private MessageResult result(Message message, SavedReply reply) {
        return new MessageResult(message.id(), message.conversationId(), reply);
    }
}
