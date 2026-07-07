package com.sbqs.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "authentication_audits")
@Getter
@Setter
@NoArgsConstructor
public class AuthenticationAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long authenticationAuditId;
    private Long userId;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private boolean successful;
    private String authenticationSource;
    private String failureReason;
    private String ipAddress;
    @Column(length = 512)
    private String userAgent;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
