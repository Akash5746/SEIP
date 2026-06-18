package com.seip.auth.service;

import com.seip.auth.entity.RefreshToken;
import com.seip.auth.entity.User;
import com.seip.auth.exception.ResourceNotFoundException;
import com.seip.auth.exception.TokenExpiredException;
import com.seip.auth.exception.TokenRevokedException;
import com.seip.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-token-expiry-days}")
    private long refreshTokenExpiryDays;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(Instant.now().plusSeconds(refreshTokenExpiryDays * 24 * 60 * 60))
                .revoked(false)
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        log.debug("Created refresh token for user: {}", user.getUsername());
        return saved;
    }

    @Transactional(readOnly = true)
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isRevoked()) {
            log.warn("Attempt to use revoked refresh token for user: {}", token.getUser().getUsername());
            throw new TokenRevokedException("Refresh token has been revoked. Please log in again.");
        }
        if (token.getExpiryDate().isBefore(Instant.now())) {
            log.warn("Expired refresh token for user: {}", token.getUser().getUsername());
            throw new TokenExpiredException("Refresh token has expired. Please log in again.");
        }
        return token;
    }

    @Transactional
    public void revokeToken(String tokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ResourceNotFoundException("RefreshToken", "token", tokenValue));
        token.setRevoked(true);
        refreshTokenRepository.save(token);
        log.debug("Revoked refresh token for user: {}", token.getUser().getUsername());
    }

    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.debug("Revoked all refresh tokens for userId: {}", userId);
    }
}
