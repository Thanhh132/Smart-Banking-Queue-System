package com.sbqs.service;

import com.sbqs.config.GeocodingProperties;
import com.sbqs.dto.GeocodeResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeocodingService {
    private final RestTemplate restTemplate;
    private final GeocodingProperties properties;
    private final Map<String, GeocodeResponse> cache = new ConcurrentHashMap<>();

    public GeocodingService(RestTemplate restTemplate, GeocodingProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public GeocodeResponse geocode(String address) {
        if (address == null || address.isBlank()) {
            throw new RuntimeException("Vui long nhap dia chi can tim");
        }

        String normalizedAddress = address.trim().toLowerCase();
        GeocodeResponse cached = cache.get(normalizedAddress);
        if (cached != null) {
            return cached;
        }

        List<Map<String, Object>> results = List.of();
        List<String> candidates = addressCandidates(address);
        for (int index = 0; index < candidates.size(); index++) {
            results = search(candidates.get(index));
            if (!results.isEmpty()) {
                break;
            }
            pauseBeforeRetry(index, candidates.size());
        }

        if (results.isEmpty()) {
            throw new RuntimeException("Khong tim thay vi tri tu dia chi da nhap");
        }

        Map<String, Object> result = results.get(0);
        Map<String, Object> addressParts = addressParts(result.get("address"));
        GeocodeResponse geocodeResponse = new GeocodeResponse(
                String.valueOf(result.get("display_name")),
                Double.valueOf(String.valueOf(result.get("lat"))),
                Double.valueOf(String.valueOf(result.get("lon"))),
                firstValue(addressParts, "state", "province", "city"),
                firstValue(addressParts, "city_district", "district", "county", "city", "town"),
                firstValue(addressParts, "ward", "suburb", "quarter", "neighbourhood", "village"));
        cache.put(normalizedAddress, geocodeResponse);
        return geocodeResponse;
    }

    private List<Map<String, Object>> search(String address) {
        URI uri = UriComponentsBuilder.fromUriString(properties.getNominatimUrl())
                .queryParam("q", address)
                .queryParam("format", "jsonv2")
                .queryParam("limit", 1)
                .queryParam("countrycodes", "vn")
                .queryParam("addressdetails", 1)
                .queryParam("accept-language", "vi")
                .build()
                .encode()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, properties.getUserAgent());

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {
                });

        return response.getBody() == null ? List.of() : response.getBody();
    }

    private List<String> addressCandidates(String address) {
        String cleaned = address.trim()
                .replaceAll("(?i)^address\\s*:\\s*", "")
                .replaceAll(",\\s*\\d{5,6}(?=\\s*,|$)", "")
                .replaceAll("\\s+", " ");

        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(cleaned);

        String[] parts = cleaned.split("\\s*,\\s*");
        if (parts.length >= 4 && !parts[0].matches(".*\\d.*")) {
            List<String> withoutPlaceName = new ArrayList<>(List.of(parts));
            withoutPlaceName.remove(0);
            candidates.add(String.join(", ", withoutPlaceName));
        }

        return candidates.stream().filter(value -> !value.isBlank()).toList();
    }

    private void pauseBeforeRetry(int index, int candidateCount) {
        if (index >= candidateCount - 1) {
            return;
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Yeu cau tim dia chi da bi huy", exception);
        }
    }

    private Map<String, Object> addressParts(Object value) {
        if (!(value instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }

        Map<String, Object> values = new ConcurrentHashMap<>();
        rawMap.forEach((key, item) -> values.put(String.valueOf(key), item));
        return values;
    }

    private String firstValue(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            Object value = values.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return "";
    }
}
