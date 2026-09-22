package dev.portableagent.conversation.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityConfigTest {

    @Test
    void jwtDecoder_whenAudienceBelongsToAnotherService_shouldRejectToken() {
        var validator = new AudienceValidator("conversation-service");
        var token = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user")
                .issuer("http://issuer")
                .audience(List.of("action-service"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        assertThat(validator.validate(token).hasErrors()).isTrue();
    }

    @Test
    void identityValidator_whenTenantIsMissing_shouldRejectToken() {
        var token = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        assertThat(new IdentityValidator().validate(token).hasErrors()).isTrue();
    }
}
