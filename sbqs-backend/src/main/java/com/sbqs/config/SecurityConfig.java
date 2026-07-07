package com.sbqs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.time.Instant;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    /**
     * Decoder kép: thử xác minh JWT fallback bằng secret nội bộ trước, nếu không đúng
     * thì xác minh JWT Keycloak bằng JWKS, issuer và audience/azp của SBQS.
     */
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String keycloakIssuer,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String keycloakJwkSetUri,
            @Value("${keycloak.client-id}") String keycloakClientId,
            FallbackAuthProperties fallbackProperties) {
        NimbusJwtDecoder keycloakDecoder = NimbusJwtDecoder.withJwkSetUri(keycloakJwkSetUri).build();
        OAuth2TokenValidator<Jwt> clientValidator = jwt -> {
            boolean correctClient = keycloakClientId.equals(jwt.getClaimAsString("azp"))
                    || jwt.getAudience().contains(keycloakClientId);
            if (correctClient) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token", "JWT was not issued for the SBQS client", null));
        };
        keycloakDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(keycloakIssuer), clientValidator));

        byte[] secret = fallbackProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        NimbusJwtDecoder fallbackDecoder = NimbusJwtDecoder.withSecretKey(
                new SecretKeySpec(secret, "HmacSHA256")).build();
        fallbackDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(fallbackProperties.getIssuer()));

        return token -> {
            try {
                return fallbackDecoder.decode(token);
            } catch (JwtException ignored) {
                return keycloakDecoder.decode(token);
            }
        };
    }

    @Bean
    /** Khai báo toàn bộ ma trận phân quyền endpoint theo role và loại phiên đăng nhập. */
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {
                })
                .exceptionHandling(exceptions -> exceptions.accessDeniedHandler((request, response, denied) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    objectMapper.writeValue(response.getWriter(), Map.of(
                            "timestamp", Instant.now().toString(),
                            "status", HttpStatus.FORBIDDEN.value(),
                            "error", "Forbidden",
                            "message", "Phien dang nhap hien tai khong duoc phep thuc hien thao tac nhay cam nay."));
                }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/api/auth/verify-email",
                                "/api/auth/resend-verification",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/account/confirm-change").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Fallback sessions can read dashboards and perform normal queue work,
                        // but cannot mutate identities, roles or top-level branch configuration.
                        .requestMatchers(HttpMethod.POST, "/api/branches", "/api/users/admin-branch")
                        .access(nonFallbackWithRoles("SUPER_ADMIN"))
                        .requestMatchers(HttpMethod.POST, "/api/users/staff")
                        .access(nonFallbackWithRoles("BRANCH_ADMIN"))
                        .requestMatchers(HttpMethod.PUT, "/api/branches/**")
                        .access(nonFallbackWithRoles("SUPER_ADMIN"))
                        .requestMatchers(HttpMethod.PUT, "/api/users/**")
                        .access(nonFallbackWithRoles("SUPER_ADMIN", "BRANCH_ADMIN"))
                        .requestMatchers(HttpMethod.DELETE, "/api/branches/**")
                        .access(nonFallbackWithRoles("SUPER_ADMIN"))
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**")
                        .access(nonFallbackWithRoles("SUPER_ADMIN", "BRANCH_ADMIN"))
                        .requestMatchers("/api/import/**").access(nonFallbackWithRoles("BRANCH_ADMIN"))
                        .requestMatchers(HttpMethod.GET, "/api/branches", "/api/branches/**")
                        .hasAnyRole("SUPER_ADMIN", "BRANCH_ADMIN", "CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/branches").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/branches/**").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/branches/**").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/users/admin-branch").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/users/staff").hasRole("BRANCH_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users")
                        .hasAnyRole("SUPER_ADMIN", "BRANCH_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/**")
                        .hasAnyRole("SUPER_ADMIN", "BRANCH_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**")
                        .hasAnyRole("SUPER_ADMIN", "BRANCH_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/services")
                        .hasAnyRole("BRANCH_ADMIN", "CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/locations/geocode")
                        .hasAnyRole("SUPER_ADMIN", "CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/reports/history")
                        .hasAnyRole("SUPER_ADMIN", "BRANCH_ADMIN", "STAFF", "CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/reports/users", "/api/reports/services", "/api/reports/tickets")
                        .hasAnyRole("SUPER_ADMIN", "BRANCH_ADMIN")
                        .requestMatchers("/api/import/**").hasRole("BRANCH_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/workflows/tickets/pending-approval")
                        .hasRole("STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/services").hasRole("BRANCH_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/services/**").hasRole("BRANCH_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/services/**").hasRole("BRANCH_ADMIN")
                        .requestMatchers("/api/queue-machines", "/api/queue-machines/**").hasRole("BRANCH_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/counters/assigned").hasRole("STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/counters")
                        .hasAnyRole("BRANCH_ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/counters/{id}/assign").hasRole("STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/counters/{id}/unassign").hasRole("STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/counters").hasRole("BRANCH_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/counters/**").hasRole("BRANCH_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/counters/**").hasRole("BRANCH_ADMIN")
                        .requestMatchers("/api/queue-machine-mappings", "/api/queue-machine-mappings/**").hasRole("BRANCH_ADMIN")
                        .requestMatchers("/api/queue-monitor", "/api/queue-monitor/**")
                        .hasAnyRole("BRANCH_ADMIN", "STAFF", "CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/tickets/call-next").hasRole("STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/tickets/{ticketId}/complete").hasRole("STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/tickets/{ticketId}/cancel").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/tickets").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/tickets/current").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/tickets/*/tracking").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/tickets/status/**")
                        .hasAnyRole("BRANCH_ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/tickets").hasRole("BRANCH_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/appointments", "/api/appointments/**")
                        .hasRole("BRANCH_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/appointments").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/history", "/api/history/**")
                        .hasAnyRole("SUPER_ADMIN", "BRANCH_ADMIN", "STAFF", "CUSTOMER")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    /** Chặn JWT fallback khỏi thao tác quản trị danh tính/chi nhánh dù role trong token phù hợp. */
    private AuthorizationManager<RequestAuthorizationContext> nonFallbackWithRoles(String... roles) {
        Set<String> requiredAuthorities = Arrays.stream(roles)
                .map(role -> "ROLE_" + role)
                .collect(java.util.stream.Collectors.toSet());
        return (authentication, context) -> {
            if (authentication == null || authentication.get() == null
                    || !authentication.get().isAuthenticated()) {
                return new AuthorizationDecision(false);
            }
            Object principal = authentication.get().getPrincipal();
            boolean fallback = principal instanceof Jwt jwt
                    && "fallback".equals(jwt.getClaimAsString("token_source"));
            boolean hasRequiredRole = authentication.get().getAuthorities().stream()
                    .anyMatch(authority -> requiredAuthorities.contains(authority.getAuthority()));
            return new AuthorizationDecision(!fallback && hasRequiredRole);
        };
    }

    private Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractRealmRoles);
        return converter;
    }

    /** Chuyển realm_access.roles trong JWT thành authority ROLE_* của Spring Security. */
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .flatMap(role -> {
                    if ("ADMIN_BRANCH".equals(role)) {
                        return List.of(
                                new SimpleGrantedAuthority("ROLE_ADMIN_BRANCH"),
                                new SimpleGrantedAuthority("ROLE_BRANCH_ADMIN")).stream();
                    }

                    return List.of(new SimpleGrantedAuthority("ROLE_" + role)).stream();
                })
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
