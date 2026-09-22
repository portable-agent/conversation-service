package dev.portableagent.conversation.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RestActionClientTest {

    private MockRestServiceServer server;
    private RestActionClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("http://action");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestActionClient(builder.build());
    }

    @Test
    void create_whenActionIsSaved_shouldReturnAuthoritativePayload() {
        server.expect(requestTo("http://action/api/v1/actions"))
                .andExpect(header("Authorization", "Bearer user-token"))
                .andExpect(jsonPath("$.requestKey").value("telegram:1"))
                .andRespond(withSuccess(response(), MediaType.APPLICATION_JSON));
        var proposal = new Proposal(
                "calendar.create_event",
                "fake-calendar",
                Map.of(
                        "title", "Встреча",
                        "startAt", "2030-09-08T12:00:00+03:00",
                        "endAt", "2030-09-08T12:30:00+03:00",
                        "timeZone", "Europe/Moscow"));

        var saved = client.create(proposal, "telegram:1", "user-token");

        assertThat(saved.payloadHash()).isEqualTo("a".repeat(64));
        assertThat(saved.payload()).containsEntry("title", "Встреча");
        server.verify();
    }

    private String response() {
        return """
                {
                  "id": "50000000-0000-0000-0000-000000000001",
                  "status": "AWAITING_APPROVAL",
                  "kind": "calendar.create_event",
                  "connector": "fake-calendar",
                  "payload": {
                    "title": "Встреча",
                    "startAt": "2030-09-08T12:00:00+03:00",
                    "endAt": "2030-09-08T12:30:00+03:00",
                    "timeZone": "Europe/Moscow"
                  },
                  "payloadHash": "%s",
                  "createdAt": "2030-09-08T09:00:00Z",
                  "updatedAt": "2030-09-08T09:00:00Z"
                }
                """.formatted("a".repeat(64));
    }
}
