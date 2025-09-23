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

import lk.ijse.edu.golankacourier.dto.dashboard.DashboardDto;
import lk.ijse.edu.golankacourier.entity.User;
import lk.ijse.edu.golankacourier.service.DashboardService;
import lk.ijse.edu.golankacourier.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;

    @GetMapping("/customer")
    public ResponseEntity<DashboardDto> getCustomerDashboard(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(name = "notifLimit", defaultValue = "5") int notifLimit) {

        if (ud == null || ud.getUsername() == null) return ResponseEntity.status(401).build();
        User user = userService.findByEmail(ud.getUsername());
        DashboardDto dto = dashboardService.getDashboardForUser(user, notifLimit);
        return ResponseEntity.ok(dto);
    }
}
