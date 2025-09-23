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

    /**
     * GET /api/notifications?limit=5
     */
    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDto>> listNotifications(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(name = "limit", defaultValue = "5") int limit) {

        if (ud == null || ud.getUsername() == null) return ResponseEntity.status(401).build();
        User user = userService.findByEmail(ud.getUsername());
        List<NotificationDto> items = notificationService.getRecentForUser(user.getId(), limit);
        return ResponseEntity.ok(items);
    }

    /**
     * POST /api/notifications/mark-read  { id: 123 }
     */
    @PostMapping("/notifications/mark-read")
    public ResponseEntity<Map<String, Object>> markRead(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody Map<String, Object> body) {

        if (ud == null || ud.getUsername() == null) return ResponseEntity.status(401).build();
        User user = userService.findByEmail(ud.getUsername());
        Object idObj = body.get("id");
        if (idObj == null) return ResponseEntity.badRequest().body(Map.of("message", "id required"));
        Long id = Long.valueOf(String.valueOf(idObj));
        int updated = notificationService.markRead(user.getId(), id);
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    /**
     * POST /api/notifications/mark-all-read
     */
    @PostMapping("/notifications/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllRead(@AuthenticationPrincipal UserDetails ud) {
        if (ud == null || ud.getUsername() == null) return ResponseEntity.status(401).build();
        User user = userService.findByEmail(ud.getUsername());
        int updated = notificationService.markAllRead(user.getId());
        return ResponseEntity.ok(Map.of("updated", updated));
    }
}
