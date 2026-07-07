package com.sbqs.repository;

import com.sbqs.entity.AccountChangeToken;
import com.sbqs.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountChangeTokenRepository extends JpaRepository<AccountChangeToken, Long> {
    Optional<AccountChangeToken> findByCurrentEmailTokenHash(String tokenHash);
    Optional<AccountChangeToken> findByNewEmailTokenHash(String tokenHash);
    void deleteByUser(User user);
}
