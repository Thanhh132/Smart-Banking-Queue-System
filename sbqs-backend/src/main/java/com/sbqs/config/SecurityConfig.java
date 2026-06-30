package com.sbqs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {
                })
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
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

    private Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractRealmRoles);
        return converter;
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess == null || realmAccess.get("roles") == null) {
            return List.of();
        }

        List<String> roles = (List<String>) realmAccess.get("roles");

        return roles.stream()
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
