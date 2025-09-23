package lk.ijse.edu.golankacourier.service.auth;

/**
 * --------------------------------------------
 * @Author Dimantha Kaveen
 * @GitHub: https://github.com/KaveenDK
 * --------------------------------------------
 * @Created 8/30/2025
 * @Project GoLankaCourier
 * --------------------------------------------
 **/

import lk.ijse.edu.golankacourier.dto.auth.LoginResponseDto;
import lk.ijse.edu.golankacourier.dto.auth.TokenRefreshDto;
import lk.ijse.edu.golankacourier.dto.auth.AuthRequestDto;
import lk.ijse.edu.golankacourier.dto.auth.AuthResponseDto;
import lk.ijse.edu.golankacourier.dto.user.RegisterCustomerDto;
import lk.ijse.edu.golankacourier.entity.RefreshToken;
import lk.ijse.edu.golankacourier.entity.Role;
import lk.ijse.edu.golankacourier.entity.User;
import lk.ijse.edu.golankacourier.repository.RoleRepository;
import lk.ijse.edu.golankacourier.repository.UserRepository;
import lk.ijse.edu.golankacourier.service.EmailService;
import lk.ijse.edu.golankacourier.config.JwtProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletResponse;

/**
 * AuthService - core authentication operations: signup, login, refresh, logout.
 *
 * Notes:
 * - This service expects a JwtProvider bean with a method `String generateAccessToken(User user)` that
 *   creates a signed JWT for the given user.
 * - Refresh tokens are managed by TokenService (persisted in DB). Refresh tokens are set to client as
 *   an HttpOnly cookie by this service.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final JwtProvider jwtProvider;
    private final EmailService emailService; // used for verification, forgot-password, etc.

    @Value("${jwt.access-token-exp-ms:900000}")
    private long accessTokenExpiryMs;

    @Value("${app.mail.from:}")
    private String mailFrom;

    // -------------------------
    // Registration / verification
    // -------------------------

    @Transactional
    public AuthResponseDto registerCustomer(RegisterCustomerDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            return new AuthResponseDto(false, "Email already in use");
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            return new AuthResponseDto(false, "Passwords do not match");
        }

        User user = User.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail().toLowerCase(Locale.ROOT))
                .phone(dto.getPhone())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .enabled(false) // not enabled until email verification
                .build();

        // set role CUSTOMER
        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_CUSTOMER").build()));
        user.getRoles().add(customerRole);

        userRepository.save(user);

        // generate verification code and send email
        String code = generateVerificationCode();
        // TODO: persist verification code with expiry (DB table) rather than relying on memory.
        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), code);

        return new AuthResponseDto(true, "Registration successful. Verification code sent to email.");
    }

    private String generateVerificationCode() {
        // simple 6-digit code
        int r = new Random().nextInt(900000) + 100000;
        return String.valueOf(r);
    }

    /**
     * Verify account using a code.
     * NOTE: This method assumes you have a persistent store for verification codes. We leave persistence out
     * from this example and only show the method signature that should be called from your controller.
     */
    @Transactional
    public AuthResponseDto verifyEmail(String email, String code) {
        // TODO: Implement verification token lookup; this is a placeholder.
        // Example:
        //  VerificationToken token = verificationTokenRepo.findValidTokenByEmail(email, code);
        //  if token == null -> return failure; else set user.enabled = true
        Optional<User> maybe = userRepository.findByEmail(email);
        if (maybe.isEmpty()) {
            return new AuthResponseDto(false, "No user found for email");
        }
        User user = maybe.get();
        // TODO: check code validity here
        user.setEnabled(true);
        userRepository.save(user);
        return new AuthResponseDto(true, "Email verified successfully");
    }

    // -------------------------
    // Login / Logout / Refresh
    // -------------------------

    /**
     * Authenticate user credentials, issue access token & refresh cookie.
     *
     * @param req      AuthRequestDto with email & password
     * @param response HttpServletResponse used to set HttpOnly refresh token cookie
     * @return LoginResponseDto containing access token and metadata
     */
    @Transactional
    public LoginResponseDto login(AuthRequestDto req, HttpServletResponse response) {
        String email = req.getEmail().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!user.isEnabled()) {
            throw new IllegalStateException("Account not active. Please verify your email or wait for approval.");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        // generate access token (JWT)
        String accessToken = jwtProvider.generateAccessToken(user); // implement this method in JwtProvider
        long expiresInSeconds = Duration.ofMillis(accessTokenExpiryMs).getSeconds();

        // create refresh token (persisted)
        String refreshToken = tokenService.createRefreshToken(user);

        // set refresh token as HttpOnly cookie
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true) // set to true in production (HTTPS)
                .path("/")
                .maxAge(Duration.ofMillis(tokenService.getRefreshTokenExpiryMs()).getSeconds())
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        return new LoginResponseDto(accessToken, "Bearer", expiresInSeconds, roles);
    }

    /**
     * Rotate/refresh access token using refresh token (cookie value).
     *
     * @param incomingRefreshToken raw token (value from cookie)
     * @param response             HttpServletResponse to set new refresh cookie (rotated token)
     * @return TokenRefreshDto containing new access token and expiry
     */
    @Transactional
    public TokenRefreshDto refreshAccessToken(String incomingRefreshToken, HttpServletResponse response) {
        if (!StringUtils.hasText(incomingRefreshToken)) {
            throw new IllegalArgumentException("Refresh token is required");
        }

        RefreshToken stored = tokenService.validateRefreshToken(incomingRefreshToken);

        // rotate refresh token (create a new one and revoke old)
        String newRefreshToken = tokenService.rotateRefreshToken(incomingRefreshToken);

        // set cookie with rotated token
        ResponseCookie cookie = ResponseCookie.from("refresh_token", newRefreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofMillis(tokenService.getRefreshTokenExpiryMs()).getSeconds())
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // create new access token
        User user = stored.getUser();
        String newAccessToken = jwtProvider.generateAccessToken(user);

        long expiresInSeconds = Duration.ofMillis(accessTokenExpiryMs).getSeconds();
        return new TokenRefreshDto(newAccessToken, "Bearer", expiresInSeconds);
    }

    /**
     * Logout: revoke refresh token(s) and clear cookie.
     *
     * @param refreshToken value of the cookie (may be null)
     * @param response     used to clear cookie
     */
    @Transactional
    public void logout(String refreshToken, HttpServletResponse response) {
        if (StringUtils.hasText(refreshToken)) {
            try {
                tokenService.revokeRefreshToken(refreshToken);
            } catch (Exception ignored) { }
        }
        // clear cookie
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // -------------------------
    // Password reset flows (skeletons)
    // -------------------------

    /**
     * Initiate forgot-password flow. Sends email with reset link/token.
     */
    public void forgotPassword(String email) {
        Optional<User> maybe = userRepository.findByEmail(email);
        if (maybe.isEmpty()) {
            // don't disclose user existence; optionally log
            return;
        }
        User user = maybe.get();
        String token = UUID.randomUUID().toString();
        // TODO: persist a password-reset token entity with expiry
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), token);
    }

    /**
     * Reset password using token (skeleton).
     */
    @Transactional
    public AuthResponseDto resetPassword(String token, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            return new AuthResponseDto(false, "Passwords do not match");
        }
        // TODO: validate reset token and find associated user
        // Example:
        // PasswordResetToken prt = passwordResetRepo.findByToken(token);
        // if invalid -> return failure
        // else update user's password and delete token
        return new AuthResponseDto(true, "Password reset successful (TODO: implement token validation)");
    }
}
