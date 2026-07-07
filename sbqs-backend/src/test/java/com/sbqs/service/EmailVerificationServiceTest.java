package com.sbqs.service;

import com.sbqs.config.EmailVerificationProperties;
import com.sbqs.entity.EmailVerificationToken;
import com.sbqs.entity.User;
import com.sbqs.repository.EmailVerificationTokenRepository;
import com.sbqs.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailVerificationServiceTest {
    @Test
    void activatesLocalAndKeycloakAccountsForAValidToken() throws Exception {
        EmailVerificationTokenRepository tokenRepository = mock(EmailVerificationTokenRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        KeycloakService keycloakService = mock(KeycloakService.class);
        AuthenticationMailService mailService = mock(AuthenticationMailService.class);

        User user = new User();
        user.setUserId(1L);
        user.setEmail("customer@example.com");
        user.setStatus("PENDING");
        user.setKeycloakUserId("kc-1");

        String rawToken = "verification-token";
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        when(tokenRepository.findByTokenHash(hash(rawToken))).thenReturn(Optional.of(token));

        EmailVerificationService service = new EmailVerificationService(
                tokenRepository,
                userRepository,
                keycloakService,
                new EmailVerificationProperties(),
                mailService);

        service.verify(rawToken);

        assertEquals("ACTIVE", user.getStatus());
        assertNotNull(token.getUsedAt());
        verify(keycloakService).verifyUserEmail("kc-1");
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    void treatsRepeatedVerificationAsSuccessWhenAccountIsAlreadyActive() throws Exception {
        EmailVerificationTokenRepository tokenRepository = mock(EmailVerificationTokenRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        KeycloakService keycloakService = mock(KeycloakService.class);
        AuthenticationMailService mailService = mock(AuthenticationMailService.class);

        User user = new User();
        user.setStatus("ACTIVE");
        user.setKeycloakUserId("kc-1");

        String rawToken = "already-used-token";
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        token.setUsedAt(LocalDateTime.now().minusMinutes(2));
        when(tokenRepository.findByTokenHash(hash(rawToken))).thenReturn(Optional.of(token));

        EmailVerificationService service = new EmailVerificationService(
                tokenRepository,
                userRepository,
                keycloakService,
                new EmailVerificationProperties(),
                mailService);

        service.verify(rawToken);

        verify(keycloakService, org.mockito.Mockito.never()).verifyUserEmail("kc-1");
        verify(userRepository, org.mockito.Mockito.never()).save(user);
    }

    private String hash(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }
}
