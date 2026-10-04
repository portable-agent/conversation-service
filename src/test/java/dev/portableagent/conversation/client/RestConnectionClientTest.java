package dev.portableagent.conversation.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RestConnectionClientTest {

    private MockRestServiceServer server;
    private RestConnectionClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("http://connections");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestConnectionClient(builder.build());
    }

    @Test
    void status_whenOneActiveConnectionExists_shouldReturnReady() {
        server.expect(requestTo("http://connections/api/v1/connections"))
                .andExpect(header("Authorization", "Bearer user-token"))
                .andRespond(withSuccess("""
                        [{
                          "id":"10000000-0000-0000-0000-000000000001",
                          "provider":"google-calendar",
                          "status":"active",
                          "createdAt":"2030-01-01T00:00:00Z"
                        }]
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.status("google-calendar", "user-token")).isEqualTo(ConnectionStatus.READY);
        server.verify();
    }

    @Test
    void status_whenConnectionIsMissing_shouldReturnMissing() {
        server.expect(requestTo("http://connections/api/v1/connections"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.status("google-calendar", "user-token")).isEqualTo(ConnectionStatus.MISSING);
    }

    @Test
    void status_whenResponseIsIncomplete_shouldFailClosed() {
        server.expect(requestTo("http://connections/api/v1/connections"))
                .andRespond(withSuccess("[{\"provider\":\"google-calendar\"}]", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.status("google-calendar", "user-token"))
                .isInstanceOf(ConnectionUnavailable.class);
    }

    @Test
    void start_shouldReturnShortLivedAuthorizationUrl() {
        server.expect(requestTo("http://connections/api/v1/connections/start"))
                .andExpect(header("Authorization", "Bearer user-token"))
                .andExpect(content().json("{\"provider\":\"google-calendar\"}"))
                .andRespond(withSuccess("""
                        {"url":"https://accounts.google.com/oauth","expiresAt":"2030-01-01T00:05:00Z"}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.start("google-calendar", "user-token"))
                .isEqualTo(URI.create("https://accounts.google.com/oauth"));
    }

    @Test
    void start_whenProviderIsNotConfigured_shouldHideRemoteDetails() {
        server.expect(requestTo("http://connections/api/v1/connections/start"))
                .andRespond(withBadRequest().body("secret remote error"));

        assertThatThrownBy(() -> client.start("google-calendar", "user-token"))
                .isInstanceOf(ConnectionUnavailable.class)
                .hasMessage("Connection Service is unavailable");
    }
}
