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

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lk.ijse.edu.golankacourier.dto.auth.*;
import lk.ijse.edu.golankacourier.dto.user.PasswordResetDto;
import lk.ijse.edu.golankacourier.dto.user.RegisterCustomerDto;
import lk.ijse.edu.golankacourier.exception.ApiException;
import lk.ijse.edu.golankacourier.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup/customer")
    public ResponseEntity<AuthResponseDto> registerCustomer(@Valid @RequestBody RegisterCustomerDto dto) {
        AuthResponseDto resp = authService.registerCustomer(dto);
        return ResponseEntity.status(resp.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST).body(resp);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponseDto> verifyEmail(@RequestBody VerifyEmailRequest req) {
        AuthResponseDto resp = authService.verifyEmail(req.getEmail(), req.getCode());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody AuthRequestDto req, HttpServletResponse response) {
        try {
            LoginResponseDto dto = authService.login(req, response);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ex.getMessage());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshDto> refresh(@CookieValue(name = "refresh_token", required = false) String refreshToken,
                                                   HttpServletResponse response) {
        try {
            TokenRefreshDto dto = authService.refreshAccessToken(refreshToken, response);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refresh_token", required = false) String refreshToken,
                                       HttpServletResponse response) {
        authService.logout(refreshToken, response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req.getEmail());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponseDto> resetPassword(@Valid @RequestBody PasswordResetDto dto) {
        AuthResponseDto resp = authService.resetPassword(dto.getToken(), dto.getNewPassword(), dto.getConfirmPassword());
        return ResponseEntity.ok(resp);
    }

    // Inner small DTOs for requests not created earlier
    public static class VerifyEmailRequest {
        private String email;
        private String code;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    public static class ForgotPasswordRequest {
        private String email;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
