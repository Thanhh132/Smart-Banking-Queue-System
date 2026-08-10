package com.sbqs.dto;

public record GoogleLoginConfigResponse(
        boolean enabled,
        String authorizationEndpoint,
        String clientId,
        String redirectUri) {
}
