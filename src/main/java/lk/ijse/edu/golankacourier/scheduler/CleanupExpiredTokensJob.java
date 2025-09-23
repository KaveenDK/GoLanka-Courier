package lk.ijse.edu.golankacourier.scheduler;

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
import lk.ijse.edu.golankacourier.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CleanupExpiredTokensJob {
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.token.cleanup.cron:0 0 * * * *}")
    private String cleanupCron;

    @Scheduled(cron = "${app.token.cleanup.cron:0 0 * * * *}")
    public void scheduledCleanup() {
        log.info("Running scheduled cleanup of expired/revoked refresh tokens (cron={})", cleanupCron);
        try {
            int removed = cleanupExpiredAndRevokedTokens();
            log.info("Cleanup completed: removed {} refresh token(s)", removed);
        } catch (Exception ex) {
            log.error("Error during cleanup of refresh tokens", ex);
        }
    }

    @Transactional
    public int cleanupExpiredAndRevokedTokens() {
        LocalDateTime now = LocalDateTime.now();
        List<RefreshToken> all = refreshTokenRepository.findAll();

        List<RefreshToken> toDelete = all.stream()
                .filter(rt -> rt != null &&
                        (rt.isRevoked() || (rt.getExpiresAt() != null && rt.getExpiresAt().isBefore(now))))
                .collect(Collectors.toList());

        if (toDelete.isEmpty()) {
            return 0;
        }

        refreshTokenRepository.deleteAll(toDelete);
        return toDelete.size();
    }
}
