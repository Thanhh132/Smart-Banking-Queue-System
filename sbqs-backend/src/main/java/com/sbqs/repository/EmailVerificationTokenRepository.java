package com.sbqs.repository;

import com.sbqs.entity.EmailVerificationToken;
import com.sbqs.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
    Optional<EmailVerificationToken> findFirstByUserOrderByCreatedAtDesc(User user);
    void deleteByUser(User user);
}
