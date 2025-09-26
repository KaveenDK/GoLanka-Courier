package lk.ijse.edu.golankacourier.controller;

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
import lk.ijse.edu.golankacourier.entity.User;
import lk.ijse.edu.golankacourier.service.NotificationService;
import lk.ijse.edu.golankacourier.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDto>> listNotifications(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(name = "limit", defaultValue = "5") int limit) {

        if (ud == null || ud.getUsername() == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findByEmail(ud.getUsername());
        List<NotificationDto> items = notificationService.getRecentForUser(user.getId(), limit);
        return ResponseEntity.ok(items);
    }

    @PostMapping("/notifications/mark-read")
    public ResponseEntity<Map<String, Object>> markRead(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody Map<String, Object> body) {

        if (ud == null || ud.getUsername() == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userService.findByEmail(ud.getUsername());

        Object idObj = body.get("id");
        if (idObj == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "id required"));
        }

        final Long id;
        try {
            id = Long.valueOf(String.valueOf(idObj));
        } catch (NumberFormatException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", "id must be a number"));
        }

        int updated = notificationService.markRead(user.getId(), id);

        if (updated <= 0) {
            // nothing was updated — either doesn't exist or already read / belongs to another user
            return ResponseEntity.status(404).body(Map.of(
                    "updated", updated,
                    "message", "No notification marked as read (not found or not owned by user)"
            ));
        }

        return ResponseEntity.ok(Map.of("updated", updated));
    }

    @PostMapping("/notifications/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllRead(@AuthenticationPrincipal UserDetails ud) {
        if (ud == null || ud.getUsername() == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userService.findByEmail(ud.getUsername());
        int updated = notificationService.markAllRead(user.getId());
        return ResponseEntity.ok(Map.of("updated", updated));
    }
}
