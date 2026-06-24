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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeocodingService {
    private static final Pattern GOOGLE_AT_COORDINATES = Pattern.compile("@(-?\\d+(?:\\.\\d+)?),\\s*(-?\\d+(?:\\.\\d+)?)");
    private static final Pattern GOOGLE_BANG_COORDINATES = Pattern.compile("!3d(-?\\d+(?:\\.\\d+)?)!4d(-?\\d+(?:\\.\\d+)?)");
    private static final Pattern PLAIN_COORDINATES = Pattern.compile("(^|\\s|,)(-?\\d{1,2}(?:\\.\\d+)?),\\s*(-?\\d{1,3}(?:\\.\\d+)?)(\\s|,|$)");

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

        GeocodeResponse coordinateResult = fromCoordinates(address);
        if (coordinateResult != null) {
            cache.put(normalizedAddress, coordinateResult);
            return coordinateResult;
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
                .queryParam("limit", 5)
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
                .replaceAll("https?://\\S+", "")
                .replaceAll(",\\s*\\d{5,6}(?=\\s*,|$)", "")
                .replaceAll("(?i)\\b(tp\\.?|t\\.p\\.)\\b", "Thành phố")
                .replaceAll("(?i)\\b(q\\.)\\b", "Quận")
                .replaceAll("(?i)\\b(p\\.)\\b", "Phường")
                .replaceAll("\\s+", " ");

        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(cleaned);

        String[] parts = cleaned.split("\\s*,\\s*");
        if (parts.length >= 2) {
            for (int index = 1; index < parts.length; index++) {
                candidates.add(String.join(", ", List.of(parts).subList(index, parts.length)));
            }
        }

        if (!cleaned.toLowerCase().contains("việt nam")
                && !cleaned.toLowerCase().contains("viet nam")) {
            candidates.add(cleaned + ", Việt Nam");
        }

        return candidates.stream().filter(value -> !value.isBlank()).toList();
    }

    private GeocodeResponse fromCoordinates(String value) {
        Matcher googleBangMatcher = GOOGLE_BANG_COORDINATES.matcher(value);
        if (googleBangMatcher.find()) {
            return coordinateResponse(googleBangMatcher.group(1), googleBangMatcher.group(2));
        }

        Matcher googleAtMatcher = GOOGLE_AT_COORDINATES.matcher(value);
        if (googleAtMatcher.find()) {
            return coordinateResponse(googleAtMatcher.group(1), googleAtMatcher.group(2));
        }

        Matcher plainMatcher = PLAIN_COORDINATES.matcher(value.trim());
        if (plainMatcher.find()) {
            return coordinateResponse(plainMatcher.group(2), plainMatcher.group(3));
        }

        return null;
    }

    private GeocodeResponse coordinateResponse(String latitudeValue, String longitudeValue) {
        double latitude = Double.parseDouble(latitudeValue);
        double longitude = Double.parseDouble(longitudeValue);
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new RuntimeException("Toa do khong hop le");
        }
        return new GeocodeResponse(
                latitude + ", " + longitude,
                latitude,
                longitude,
                "",
                "",
                "");
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
