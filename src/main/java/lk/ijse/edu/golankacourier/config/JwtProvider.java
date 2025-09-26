package lk.ijse.edu.golankacourier.config;

/**
 * --------------------------------------------
 * @Author Dimantha Kaveen
 * @GitHub: https://github.com/KaveenDK
 * --------------------------------------------
 * @Created 8/30/2025
 * @Project GoLankaCourier
 * --------------------------------------------
 **/

import lk.ijse.edu.golankacourier.entity.User;
import lk.ijse.edu.golankacourier.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtUtil jwtUtil;

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

