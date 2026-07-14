package com.sbqs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sbqs.smart-routing")
public class SmartRoutingProperties {
    private double distanceWeight = 0.4;
    private double waitWeight = 0.6;
    private long noActiveCounterPenaltyMinutes = 30;
}
