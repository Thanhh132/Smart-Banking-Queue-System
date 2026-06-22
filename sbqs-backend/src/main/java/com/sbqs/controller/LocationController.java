package com.sbqs.controller;

import com.sbqs.dto.GeocodeResponse;
import com.sbqs.service.GeocodingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/locations")
public class LocationController {
    private final GeocodingService geocodingService;

    public LocationController(GeocodingService geocodingService) {
        this.geocodingService = geocodingService;
    }

    @GetMapping("/geocode")
    public ResponseEntity<GeocodeResponse> geocode(@RequestParam String address) {
        return ResponseEntity.ok(geocodingService.geocode(address));
    }
}
