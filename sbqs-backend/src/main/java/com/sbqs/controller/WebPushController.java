package com.sbqs.controller;

import com.sbqs.dto.push.PushPublicKeyResponse;
import com.sbqs.dto.push.PushSubscriptionRequest;
import com.sbqs.dto.push.PushSubscriptionResponse;
import com.sbqs.service.WebPushService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/push")
public class WebPushController {
    private final WebPushService webPushService;

    public WebPushController(WebPushService webPushService) {
        this.webPushService = webPushService;
    }

    @GetMapping("/public-key")
    public ResponseEntity<PushPublicKeyResponse> getPublicKey() {
        return ResponseEntity.ok(webPushService.getPublicKey());
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<PushSubscriptionResponse> subscribe(
            @Valid @RequestBody PushSubscriptionRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(webPushService.subscribe(request, httpRequest.getHeader("User-Agent")));
    }

    @DeleteMapping("/subscriptions")
    public ResponseEntity<PushSubscriptionResponse> unsubscribe(
            @Valid @RequestBody PushSubscriptionRequest request) {
        return ResponseEntity.ok(webPushService.unsubscribe(request));
    }
}
