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

import lk.ijse.edu.golankacourier.dto.customer.ActivityDto;
import lk.ijse.edu.golankacourier.dto.customer.CustomerStatsDto;
import lk.ijse.edu.golankacourier.dto.customer.ParcelSummaryDto;
import lk.ijse.edu.golankacourier.entity.User;
import lk.ijse.edu.golankacourier.service.DashboardService;
import lk.ijse.edu.golankacourier.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerDashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;

    private User findUserFromPrincipal(UserDetails ud) {
        if (ud == null || ud.getUsername() == null) return null;
        return userService.findByEmail(ud.getUsername());
    }

    @GetMapping("/stats")
    public CustomerStatsDto getStats(@AuthenticationPrincipal UserDetails ud) {
        User user = findUserFromPrincipal(ud);
        if (user == null) throw new IllegalStateException("User not authenticated");
        return dashboardService.getStats(user.getId());
    }

    @GetMapping("/deliveries")
    public List<ParcelSummaryDto> listDeliveries(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {

        User user = findUserFromPrincipal(ud);
        if (user == null) throw new IllegalStateException("User not authenticated");
        return dashboardService.listDeliveries(user.getId(), status, limit);
    }

    @GetMapping("/history")
    public List<ParcelSummaryDto> recentHistory(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {

        User user = findUserFromPrincipal(ud);
        if (user == null) throw new IllegalStateException("User not authenticated");
        return dashboardService.recentHistory(user.getId(), limit);
    }

    @GetMapping("/activity")
    public List<ActivityDto> recentActivity(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {

        User user = findUserFromPrincipal(ud);
        if (user == null) throw new IllegalStateException("User not authenticated");
        return dashboardService.recentActivity(user.getId(), limit);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal UserDetails ud) {
        User user = findUserFromPrincipal(ud);
        if (user == null) throw new IllegalStateException("User not authenticated");
        return dashboardService.registerSseEmitter(user.getId());
    }
}
