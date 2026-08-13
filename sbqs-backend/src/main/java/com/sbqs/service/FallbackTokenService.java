package com.sbqs.service;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.sbqs.config.FallbackAuthProperties;
import com.sbqs.entity.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class FallbackTokenService {
    private final FallbackAuthProperties properties;
    private final JwtEncoder encoder;

    public FallbackTokenService(FallbackAuthProperties properties) {
        this.properties = properties;
        if (properties.getSecret() == null || properties.getSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("SBQS_FALLBACK_JWT_SECRET must contain at least 32 bytes");
        }
        this.encoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(properties.getSecret().getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Cấp JWT HS256 sống ngắn từ thông tin user nội bộ khi Keycloak bị down.
     * Claim token_source=fallback được SecurityConfig dùng để khóa thao tác nhạy cảm.
     */
    public String issue(User user) {
        return issue(user, "fallback");
    }

    /** Local-development impersonation token; guarded by DevLoginController loopback checks. */
    public String issueDevelopment(User user) {
        return issue(user, "dev_quick_login");
    }

    private String issue(User user, String tokenSource) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(properties.getAccessTokenMinutes(), ChronoUnit.MINUTES))
                .subject(String.valueOf(user.getUserId()))
                .claim("email", user.getEmail())
                .claim("preferred_username", user.getEmail())
                .claim("name", user.getFullName())
                .claim("realm_access", Map.of("roles", List.of(user.getRole())))
                .claim("token_source", tokenSource)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public int expiresInSeconds() {
        return Math.toIntExact(properties.getAccessTokenMinutes() * 60);
    }
}
