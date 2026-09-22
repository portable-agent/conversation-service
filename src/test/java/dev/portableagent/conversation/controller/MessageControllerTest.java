package dev.portableagent.conversation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.portableagent.conversation.client.AgentUnavailable;
import dev.portableagent.conversation.model.ReplyType;
import dev.portableagent.conversation.model.SavedReply;
import dev.portableagent.conversation.service.MessageFlowService;
import dev.portableagent.conversation.service.MessageResult;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = MessageController.class,
        properties = {
            "OIDC_ISSUER=http://localhost/test-issuer",
            "OIDC_JWKS_URL=http://localhost/test-jwks",
            "OIDC_AUDIENCE=conversation-service"
        })
@Import({dev.portableagent.conversation.config.SecurityConfig.class, ApiErrorHandler.class})
class MessageControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MessageFlowService service;

    @MockitoBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @Test
    void createMessage_withJwt_shouldUseJwtIdentityAndReturnText() throws Exception {
        var messageId = UUID.randomUUID();
        var conversationId = UUID.randomUUID();
        var tenantId = UUID.randomUUID();
        when(service.handle(argThat(command -> command.storeCommand().tenantId().equals(tenantId)
                        && command.storeCommand().subject().equals("user-42")
                        && command.accessToken().equals("user-token"))))
                .thenReturn(new MessageResult(
                        messageId, conversationId, new SavedReply(ReplyType.TEXT, Map.of("text", "Когда начать?"))));

        mvc.perform(post("/api/v1/messages")
                        .with(jwt().jwt(token -> token.tokenValue("user-token")
                                .subject("user-42")
                                .claim("tenant_id", tenantId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestKey": "telegram:100",
                                  "text": "Создай встречу",
                                  "context": {"locale": "ru-RU", "timeZone": "Europe/Moscow"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value(messageId.toString()))
                .andExpect(jsonPath("$.reply.type").value("text"))
                .andExpect(jsonPath("$.reply.text").value("Когда начать?"));
    }

    @Test
    void createMessage_withoutJwt_shouldReturnUnauthorized() throws Exception {
        mvc.perform(post("/api/v1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createMessage_withInvalidBody_shouldReturnUnprocessableEntity() throws Exception {
        mvc.perform(post("/api/v1/messages")
                        .with(jwt().jwt(token -> token.subject("user-42")
                                .claim("tenant_id", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Request cannot be processed"));
    }

    @Test
    void createMessage_whenAgentIsUnavailable_shouldReturnBadGateway() throws Exception {
        when(service.handle(any())).thenThrow(new AgentUnavailable());

        mvc.perform(post("/api/v1/messages")
                        .with(jwt().jwt(token -> token.subject("user-42")
                                .claim("tenant_id", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestKey": "telegram:100",
                                  "text": "Создай встречу",
                                  "context": {"locale": "ru-RU", "timeZone": "Europe/Moscow"}
                                }
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("Dependency is temporarily unavailable"));
    }
}
