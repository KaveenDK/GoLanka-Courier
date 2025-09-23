package lk.ijse.edu.golankacourier.service.impl;

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
import lk.ijse.edu.golankacourier.entity.User;
import lk.ijse.edu.golankacourier.repository.NotificationRepository;
import lk.ijse.edu.golankacourier.repository.UserRepository;
import lk.ijse.edu.golankacourier.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    public List<NotificationDto> getRecentForUser(Long userId, int limit) {
        var page = PageRequest.of(0, Math.max(1, Math.min(200, limit)));
        List<Notification> list = notificationRepository.findByUserIdOrderByTimestampDesc(userId, page);
        return list.stream().map(NotificationDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public int markRead(Long userId, Long notificationId) {
        return notificationRepository.markReadByIdForUser(notificationId, userId);
    }

    @Override
    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllReadForUser(userId);
    }

    @Override
    public Notification createForUser(Long userId, String title, String message) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Notification n = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .isRead(false)
                .build();
        return notificationRepository.save(n);
    }
}
