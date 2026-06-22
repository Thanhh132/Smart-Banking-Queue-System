package com.sbqs.repository;

import com.sbqs.entity.PasswordResetToken;
import com.sbqs.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findFirstByUserOrderByCreatedAtDesc(User user);

    void deleteByUser(User user);
}
