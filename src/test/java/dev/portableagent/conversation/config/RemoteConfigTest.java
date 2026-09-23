package dev.portableagent.conversation.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import dev.portableagent.conversation.model.Message;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class RemoteConfigTest {

    @Test
    void agentClient_whenCallingPythonService_shouldUseHttp11() throws Exception {
        var upgrade = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/proposals", exchange -> {
            upgrade.set(exchange.getRequestHeaders().getFirst("Upgrade"));
            var body = """
                    {
                      "proposal": null,
                      "clarification": {
                        "question": "Когда начать?",
                        "missingFields": ["startAt"]
                      }
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try {
            var baseUrl = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            var properties = new RemoteProperties(baseUrl, baseUrl, Duration.ofSeconds(1), Duration.ofSeconds(1));
            var client = new RemoteConfig().agentClient(properties);

            var reply = client.ask(message(), "user-token");

            assertThat(reply.question()).isEqualTo("Когда начать?");
            assertThat(upgrade.get()).isNull();
        } finally {
            server.stop(0);
        }
    }

    private Message message() {
        return new Message(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "user-42",
                "smoke:1",
                "Создай встречу",
                "ru-RU",
                "Europe/Moscow",
                Instant.parse("2026-09-16T10:00:00Z"),
                null);
    }
}
