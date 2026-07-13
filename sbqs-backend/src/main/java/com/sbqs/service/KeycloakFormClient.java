package com.sbqs.service;

import com.sbqs.exception.KeycloakUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/** Client nội bộ dùng chung cho các endpoint form của Keycloak và chuẩn hóa xử lý lỗi kết nối. */
@Component
class KeycloakFormClient {
    private static final Logger log = LoggerFactory.getLogger(KeycloakFormClient.class);

    private final RestTemplate restTemplate;

    KeycloakFormClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    Map<String, Object> postForMap(String url, MultiValueMap<String, String> body) {
        HttpHeaders headers = formHeaders();
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            log.info("Calling Keycloak form endpoint url={}", url);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, new ParameterizedTypeReference<>() {
                    });
            if (response.getBody() == null) {
                throw new RuntimeException("Keycloak khong tra ve du lieu");
            }
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            throw new RuntimeException("Keycloak tu choi yeu cau: " + ex.getResponseBodyAsString());
        } catch (ResourceAccessException | HttpServerErrorException ex) {
            log.warn("Keycloak unavailable url={}", url, ex);
            throw new KeycloakUnavailableException("Dich vu xac thuc tam thoi khong san sang", ex);
        }
    }

    void postForVoid(String url, MultiValueMap<String, String> body) {
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, formHeaders());
        try {
            restTemplate.postForEntity(url, request, Void.class);
        } catch (HttpClientErrorException ex) {
            throw new RuntimeException("Keycloak tu choi logout: " + ex.getResponseBodyAsString());
        } catch (ResourceAccessException ex) {
            log.warn("Cannot connect to Keycloak logout endpoint url={}", url, ex);
            throw new RuntimeException("Khong ket noi duoc Keycloak de dang xuat");
        }
    }

    private HttpHeaders formHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }
}
