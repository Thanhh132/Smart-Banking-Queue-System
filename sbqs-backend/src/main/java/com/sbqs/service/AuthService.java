package com.sbqs.service;

import com.sbqs.dto.LoginRequest;
import com.sbqs.dto.RegisterRequest;
import com.sbqs.entity.User;
import com.sbqs.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public AuthService(
            UserRepository userRepository) {

        this.userRepository = userRepository;
    }

    public User register(
            RegisterRequest request) {

        if (userRepository.existsByEmail(
                request.getEmail())) {

            throw new RuntimeException(
                    "Email đã tồn tại");
        }

        User user = new User();

        user.setFullName(
                request.getFullName());

        user.setEmail(
                request.getEmail());

        user.setPhone(
                request.getPhone());

        user.setRole("CUSTOMER");

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.getPassword()));

        return userRepository.save(user);
    }

    public User login(
            LoginRequest request) {

        User user =
                userRepository.findByEmail(
                                request.getEmail())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Email không tồn tại"));

        boolean matched =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPasswordHash());

        if (!matched) {

            throw new RuntimeException(
                    "Sai mật khẩu");
        }

        return user;
    }
}