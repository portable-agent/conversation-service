package dev.portableagent.conversation.config;

import java.util.UUID;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

final class IdentityValidator implements OAuth2TokenValidator<Jwt> {

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        var subject = token.getSubject();
        var tenantId = token.getClaimAsString("tenant_id");
        if (subject == null || subject.isBlank() || !isUuid(tenantId)) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Required identity claims are missing", null));
        }
        return OAuth2TokenValidatorResult.success();
    }

    private boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException | NullPointerException exception) {
            return false;
        }
    }
}
