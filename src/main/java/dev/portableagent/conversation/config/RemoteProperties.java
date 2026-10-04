package dev.portableagent.conversation.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("remote")
public record RemoteProperties(
        @NotNull URI agentUrl,
        @NotNull URI actionUrl,
        @NotNull URI connectionUrl,
        @NotEmpty Set<String> availableConnectors,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout) {}
