package com.sbqs.controller;

import com.sbqs.config.DevLoginProperties;
import com.sbqs.dto.DevLoginAccountResponse;
import com.sbqs.dto.LoginResponse;
import com.sbqs.service.DevLoginService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/auth/dev")
public class DevLoginController {
    private static final Set<String> LOOPBACK_ADDRESSES = Set.of(
            "127.0.0.1", "::1", "0:0:0:0:0:0:0:1");
    private static final Set<String> LOOPBACK_HOSTS = Set.of(
            "localhost", "127.0.0.1", "::1", "0:0:0:0:0:0:0:1");
    private final DevLoginService devLoginService;
    private final DevLoginProperties properties;

    public DevLoginController(DevLoginService devLoginService, DevLoginProperties properties) {
        this.devLoginService = devLoginService;
        this.properties = properties;
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<DevLoginAccountResponse>> accounts(HttpServletRequest request) {
        if (!isLocalDevelopmentRequest(request)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(devLoginService.accounts());
    }

    @PostMapping("/login/{userId}")
    public ResponseEntity<LoginResponse> login(@PathVariable Long userId, HttpServletRequest request) {
        if (!isLocalDevelopmentRequest(request)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(devLoginService.login(userId));
    }

    private boolean isLocalDevelopmentRequest(HttpServletRequest request) {
        if (!properties.isEnabled()) return false;
        if (request.getHeader("Forwarded") != null || request.getHeader("X-Forwarded-For") != null) return false;
        String remoteAddress = request.getRemoteAddr();
        String serverName = request.getServerName();
        boolean loopbackAddress = remoteAddress != null && LOOPBACK_ADDRESSES.contains(remoteAddress);
        boolean loopbackHost = serverName != null && LOOPBACK_HOSTS.contains(serverName.toLowerCase());
        return loopbackAddress && loopbackHost;
    }
}
