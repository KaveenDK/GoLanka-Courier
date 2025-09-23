package lk.ijse.edu.golankacourier.service;

/**
 * --------------------------------------------
 * @Author Dimantha Kaveen
 * @GitHub: https://github.com/KaveenDK
 * --------------------------------------------
 * @Created 8/30/2025
 * @Project GoLankaCourier
 * --------------------------------------------
 **/

import lk.ijse.edu.golankacourier.dto.notification.NotificationDto;
import lk.ijse.edu.golankacourier.entity.Notification;

import java.util.List;

public interface NotificationService {
    List<NotificationDto> getRecentForUser(Long userId, int limit);
    int markRead(Long userId, Long notificationId);
    int markAllRead(Long userId);
    Notification createForUser(Long userId, String title, String message); // optional link overloads if needed
}
