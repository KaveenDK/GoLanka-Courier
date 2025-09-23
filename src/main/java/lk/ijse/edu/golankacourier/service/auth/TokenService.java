package lk.ijse.edu.golankacourier.service.auth;

/**
 * --------------------------------------------
 * @Author Dimantha Kaveen
 * @GitHub: https://github.com/KaveenDK
 * --------------------------------------------
 * @Created 8/30/2025
 * @Project GoLankaCourier
 * --------------------------------------------
 **/

import lk.ijse.edu.golankacourier.entity.RefreshToken;
import lk.ijse.edu.golankacourier.entity.User;
import lk.ijse.edu.golankacourier.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

/**
 * TokenService - manages persisted refresh tokens.
 *
 * Notes on production hardening:
 * - Consider hashing the refresh token before storing (store hash + compare hashed incoming token),
 *   to protect tokens in case DB is leaked. That implies saving findByHashedToken hash lookup method.
 * - Consider limiting tokens-per-user (one per device) or revoke-all-on-password-change.
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Refresh token lifetime in milliseconds (configured).
     */
    @Value("${jwt.refresh-token-exp-ms:2592000000}")
    private long refreshTokenExpMs;

    public long getRefreshTokenExpiryMs() {
        return refreshTokenExpMs;
    }

    /**
     * Create and persist a refresh token for the given user.
     *
     * @param user user
     * @return raw token string (caller should set cookie)
     */
    @Transactional
    public String createRefreshToken(User user) {
        String token = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis() + refreshTokenExpMs), ZoneId.systemDefault());

        RefreshToken rt = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(rt);
        return token;
    }

    /**
     * Validate a refresh token value and return stored entity.
     * Throws IllegalArgumentException if invalid or expired.
     *
     * @param token raw token value
     * @return stored RefreshToken entity
     */
    @Transactional
    public RefreshToken validateRefreshToken(String token) {
        Optional<RefreshToken> maybe = refreshTokenRepository.findByToken(token);
        if (maybe.isEmpty()) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        RefreshToken rt = maybe.get();
        if (rt.isRevoked()) {
            throw new IllegalArgumentException("Refresh token revoked");
        }
        if (rt.getExpiresAt() == null || rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token expired");
        }
        return rt;
    }

    /**
     * Rotate refresh token: revoke old token and create a new one for same user.
     *
     * @param oldToken raw token string
     * @return new raw token string
     */
    @Transactional
    public String rotateRefreshToken(String oldToken) {
        RefreshToken old = validateRefreshToken(oldToken);
        old.setRevoked(true);
        refreshTokenRepository.save(old);

        // create new token for same user
        return createRefreshToken(old.getUser());
    }

    /**
     * Revoke a specific refresh token value.
     */
    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    /**
     * Revoke all refresh tokens for a user (useful on password change).
     */
    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.findByUserAndRevokedFalse(user)
                .forEach(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }
}
