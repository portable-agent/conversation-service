package dev.portableagent.conversation.controller;

import dev.portableagent.conversation.api.MessagesApi;
import dev.portableagent.conversation.api.model.MessageRequest;
import dev.portableagent.conversation.api.model.MessageResponse;
import dev.portableagent.conversation.service.MessageFlowService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MessageController implements MessagesApi {

    private final MessageFlowService service;

    @Override
    public ResponseEntity<MessageResponse> createConversationMessage(MessageRequest request) {
        var jwt = jwt();
        var command = MessageApiMapper.command(
                request, UUID.fromString(jwt.getClaimAsString("tenant_id")), jwt.getSubject(), jwt.getTokenValue());
        return ResponseEntity.ok(MessageApiMapper.response(service.handle(command)));
    }

    private Jwt jwt() {
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt;
        }
        throw new IllegalStateException("JWT principal is required");
    }
}
