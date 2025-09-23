package lk.ijse.edu.golankacourier.dto.notification;

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
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {
    private Long id;
    private String title;
    private String message;
    private Long timestamp;
    private Boolean read;

    public static NotificationDto fromEntity(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .timestamp(n.getTimestamp() != null ? n.getTimestamp().toEpochMilli() : null)
                .read(n.isRead())
                .build();
    }
}
