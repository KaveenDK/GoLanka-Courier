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

import jakarta.validation.Valid;
import lk.ijse.edu.golankacourier.dto.payment.PaymentInitDto;
import lk.ijse.edu.golankacourier.service.PaymentService;
import lk.ijse.edu.golankacourier.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    @PostMapping("/payhere/initialize")
    public ResponseEntity<Map<String, Object>> initializePayHere(@AuthenticationPrincipal UserDetails ud,
                                                                 @Valid @RequestBody PaymentInitDto dto) {
        var user = userService.findByEmail(ud.getUsername());
        Map<String, Object> payload = paymentService.initializePayment(dto, user.getId());
        return ResponseEntity.ok(payload);
    }

    @PostMapping("/webhook/payhere")
    public ResponseEntity<String> payhereWebhook(@RequestBody Map<String, Object> payload) {
        paymentService.handleProviderWebhook(payload);
        // PayHere expects HTTP 200 and sometimes text body
        return ResponseEntity.ok("OK");
    }
}
