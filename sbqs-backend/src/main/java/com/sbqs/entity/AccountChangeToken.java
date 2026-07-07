package com.sbqs.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_change_tokens")
@Getter
@Setter
@NoArgsConstructor
public class AccountChangeToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountChangeTokenId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String pendingFullName;

    @Column(nullable = false, length = 255)
    private String pendingEmail;

    @Column(nullable = false, length = 30)
    private String pendingPhone;

    @Column(nullable = false, unique = true, length = 64)
    private String currentEmailTokenHash;

    @Column(unique = true, length = 64)
    private String newEmailTokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime currentEmailConfirmedAt;
    private LocalDateTime newEmailConfirmedAt;
    private LocalDateTime appliedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
