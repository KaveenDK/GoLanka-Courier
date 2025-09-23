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

import lk.ijse.edu.golankacourier.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Get recent notifications for a user using Pageable for limit.
     * Usage: findByUserIdOrderByTimestampDesc(userId, PageRequest.of(0, limit))
     */
    List<Notification> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    /**
     * Mark single notification as read (returns number of rows updated).
     */
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.user.id = :userId")
    int markReadByIdForUser(Long id, Long userId);

    /**
     * Mark all unread notifications for a user as read.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllReadForUser(Long userId);
}
