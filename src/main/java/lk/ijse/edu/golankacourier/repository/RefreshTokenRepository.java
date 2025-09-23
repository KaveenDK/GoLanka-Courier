package lk.ijse.edu.golankacourier.repository;

/**
 * --------------------------------------------
 * @Author Dimantha Kaveen
 * @GitHub: https://github.com/KaveenDK
 * --------------------------------------------
 * @Created 8/30/2025
 * @Project GoLankaCourier
 * --------------------------------------------
 **/

import jakarta.transaction.Transactional;
import lk.ijse.edu.golankacourier.entity.RefreshToken;
import lk.ijse.edu.golankacourier.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

/**
 * Repository for RefreshToken entity.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUserAndRevokedFalse(User user);

    void deleteByUser(User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken r WHERE r.revoked = true OR (r.expiresAt IS NOT NULL AND r.expiresAt < :now)")
    int deleteExpiredOrRevoked(@Param("now") LocalDateTime now);
}
