package dev.portableagent.conversation.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import dev.portableagent.conversation.model.Message;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RestAgentClientTest {

    private MockRestServiceServer server;
    private RestAgentClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("http://agent");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestAgentClient(builder.build());
    }

    @Test
    void ask_whenProposalIsSafe_shouldMapIt() {
        server.expect(requestTo("http://agent/api/v1/proposals"))
                .andExpect(header("Authorization", "Bearer user-token"))
                .andRespond(withSuccess(proposal(true), MediaType.APPLICATION_JSON));

        var reply = client.ask(message(), "user-token");

        assertThat(reply.proposal().kind()).isEqualTo("calendar.create_event");
        assertThat(reply.proposal().payload())
                .containsEntry("startAt", "2030-09-08T12:00:00+03:00")
                .containsEntry("endAt", "2030-09-08T12:30:00+03:00")
                .containsEntry("timeZone", "Europe/Moscow");
        server.verify();
    }

    @Test
    void ask_whenApprovalIsNotRequired_shouldRejectResponse() {
        server.expect(requestTo("http://agent/api/v1/proposals"))
                .andRespond(withSuccess(proposal(false), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.ask(message(), "user-token")).isInstanceOf(AgentUnavailable.class);
    }

    private Message message() {
        return new Message(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "user-42",
                "telegram:1",
                "Создай встречу",
                "ru-RU",
                "Europe/Moscow",
                Instant.parse("2026-09-16T10:00:00Z"),
                null);
    }

    private String proposal(boolean requiresApproval) {
        return """
                {
                  "proposal": {
                    "proposalId": "10000000-0000-0000-0000-000000000001",
                    "kind": "calendar.create_event",
                    "connector": "fake-calendar",
                    "payload": {
                      "title": "Встреча",
                      "startAt": "2030-09-08T12:00:00+03:00",
                      "endAt": "2030-09-08T12:30:00+03:00",
                      "timeZone": "Europe/Moscow"
                    },
                    "explanation": "Создать встречу",
                    "risk": "LOW",
                    "requiresApproval": %s
                  },
                  "clarification": null
                }
                """.formatted(requiresApproval);
    }
}
