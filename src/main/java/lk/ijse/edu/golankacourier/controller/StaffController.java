package lk.ijse.edu.golankacourier.controller;

/**
 * --------------------------------------------
 *
 * @Author Dimantha Kaveen
 * @GitHub: https://github.com/KaveenDK
 * --------------------------------------------
 * @Created 9/21/2025
 * @Project GoLankaCourier
 * --------------------------------------------
 **/

import lk.ijse.edu.golankacourier.entity.DriverApplication;
import lk.ijse.edu.golankacourier.service.DriverApplicationService;
import lk.ijse.edu.golankacourier.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_STAFF')")
public class StaffController {

    private final DriverApplicationService driverApplicationService;
    private final UserService userService;

    @GetMapping("/driver-apps")
    public ResponseEntity<List<DriverApplication>> listPending() {
        return ResponseEntity.ok(driverApplicationService.listPendingApplications());
    }

    @PostMapping("/driver-apps/{id}/approve")
    public ResponseEntity<DriverApplication> approve(@PathVariable Long id,
                                                     @AuthenticationPrincipal UserDetails ud,
                                                     @RequestParam(defaultValue = "false") boolean createAccount,
                                                     @RequestParam(required = false) String notes) {
        var reviewer = userService.findByEmail(ud.getUsername());
        DriverApplication updated = driverApplicationService.approveApplication(id, reviewer.getId(), notes, createAccount);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/driver-apps/{id}/decline")
    public ResponseEntity<DriverApplication> decline(@PathVariable Long id,
                                                     @AuthenticationPrincipal UserDetails ud,
                                                     @RequestParam(required = false) String notes) {
        var reviewer = userService.findByEmail(ud.getUsername());
        DriverApplication updated = driverApplicationService.declineApplication(id, reviewer.getId(), notes);
        return ResponseEntity.ok(updated);
    }
}
