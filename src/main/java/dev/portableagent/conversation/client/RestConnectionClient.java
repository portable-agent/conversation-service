package dev.portableagent.conversation.client;

import dev.portableagent.conversation.connection.api.model.Connection;
import dev.portableagent.conversation.connection.api.model.StartRequest;
import dev.portableagent.conversation.connection.api.model.StartResponse;
import java.net.URI;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@RequiredArgsConstructor
public class RestConnectionClient implements ConnectionClient {

    private final RestClient client;

    @Override
    public ConnectionStatus status(String provider, String accessToken) {
        try {
            var connections = client.get()
                    .uri("/api/v1/connections")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(Connection[].class);
            if (connections == null
                    || Arrays.stream(connections)
                            .anyMatch(connection -> connection == null
                                    || connection.getProvider() == null
                                    || connection.getStatus() == null)) {
                throw new ConnectionUnavailable();
            }
            long active = Arrays.stream(connections)
                    .filter(connection ->
                            provider.equals(connection.getProvider().getValue()))
                    .filter(connection -> connection.getStatus() == Connection.StatusEnum.ACTIVE)
                    .count();
            if (active == 0) {
                return ConnectionStatus.MISSING;
            }
            return active == 1 ? ConnectionStatus.READY : ConnectionStatus.AMBIGUOUS;
        } catch (RestClientException exception) {
            throw new ConnectionUnavailable();
        }
    }

    @Override
    public URI start(String provider, String accessToken) {
        try {
            var response = client.post()
                    .uri("/api/v1/connections/start")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new StartRequest(StartRequest.ProviderEnum.fromValue(provider)))
                    .retrieve()
                    .body(StartResponse.class);
            if (response == null
                    || response.getUrl() == null
                    || !"https".equalsIgnoreCase(response.getUrl().getScheme())) {
                throw new ConnectionUnavailable();
            }
            return response.getUrl();
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new ConnectionUnavailable();
        }
    }
}
