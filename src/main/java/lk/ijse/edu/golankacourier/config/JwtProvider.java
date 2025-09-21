package lk.ijse.edu.golankacourier.config;

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

import lk.ijse.edu.golankacourier.entity.User;
import lk.ijse.edu.golankacourier.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * JwtProvider is a thin wrapper used by services (AuthService) to generate and validate tokens.
 * It delegates to JwtUtil. This keeps a semantic boundary in services (generateAccessToken(User)).
 */
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtUtil jwtUtil;

    /**
     * Generate access token for a user entity.
     * Exposes a simple method used by AuthService.
     */
    public String generateAccessToken(User user) {
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.toList());
        // Use username as user.getEmail()
        return jwtUtil.generateToken(user.getEmail(), roles);
    }

    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    public String getUsernameFromToken(String token) {
        return jwtUtil.getUsernameFromToken(token);
    }

    public List<String> getRolesFromToken(String token) {
        return jwtUtil.getRolesFromToken(token);
    }
}

