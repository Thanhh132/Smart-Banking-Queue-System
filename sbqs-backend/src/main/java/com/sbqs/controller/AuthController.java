package com.sbqs.controller;

import com.sbqs.dto.LoginRequest;
import com.sbqs.dto.RegisterRequest;
import com.sbqs.entity.User;
import com.sbqs.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sbqs.dto.LoginResponse;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
public class AuthController {

    private final AuthService authService;

    public AuthController(
            AuthService authService) {

        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(
            @RequestBody RegisterRequest request) {

        return ResponseEntity.ok(
                authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                authService.login(request));
    }

    @PostMapping("/repair-login")
    public ResponseEntity<Void> repairLogin(
            @RequestBody LoginRequest request) {

        authService.repairLoginAccount(request);

        return ResponseEntity.noContent().build();
    }
}
