package com.sbqs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "geocoding")
public class GeocodingProperties {
    private String nominatimUrl = "https://nominatim.openstreetmap.org/search";
    private String userAgent = "SBQS/1.0";
}
