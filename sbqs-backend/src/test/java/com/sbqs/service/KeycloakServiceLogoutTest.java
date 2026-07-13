package com.sbqs.service;

import com.sbqs.config.KeycloakProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KeycloakServiceLogoutTest {

    @Test
    void revokesRefreshTokenAtKeycloakLogoutEndpoint() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        KeycloakProperties properties = new KeycloakProperties();
        properties.setServerUrl("http://localhost:8080");
        properties.setRealm("SBQS");
        properties.setClientId("sbqs-frontend");
        properties.setClientSecret("client-secret");
        KeycloakService service = new KeycloakService(properties, new KeycloakFormClient(restTemplate));

        when(restTemplate.postForEntity(
                eq("http://localhost:8080/realms/SBQS/protocol/openid-connect/logout"),
                any(HttpEntity.class),
                eq(Void.class)))
                .thenReturn(ResponseEntity.noContent().build());

        service.logout("refresh-token-value");

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(
                eq("http://localhost:8080/realms/SBQS/protocol/openid-connect/logout"),
                requestCaptor.capture(),
                eq(Void.class));

        @SuppressWarnings("unchecked")
        MultiValueMap<String, String> body =
                (MultiValueMap<String, String>) requestCaptor.getValue().getBody();
        assertEquals("sbqs-frontend", body.getFirst("client_id"));
        assertEquals("client-secret", body.getFirst("client_secret"));
        assertEquals("refresh-token-value", body.getFirst("refresh_token"));
    }
}
