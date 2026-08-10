package com.sbqs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "sbqs.web-push")
public class WebPushProperties {
    private String publicKey = "";
    private String privateKey = "";
    private String subject = "mailto:no-reply@sbqs.local";
    private int ttlSeconds = 300;
    private List<String> allowedEndpointHosts = List.of(
            "fcm.googleapis.com",
            "push.services.mozilla.com",
            "updates.push.services.mozilla.com",
            "push.apple.com",
            "notify.windows.com");

    public boolean isConfigured() {
        return publicKey != null && !publicKey.isBlank()
                && privateKey != null && !privateKey.isBlank();
    }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public int getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(int ttlSeconds) { this.ttlSeconds = ttlSeconds; }
    public List<String> getAllowedEndpointHosts() { return allowedEndpointHosts; }
    public void setAllowedEndpointHosts(List<String> allowedEndpointHosts) { this.allowedEndpointHosts = allowedEndpointHosts; }
}
