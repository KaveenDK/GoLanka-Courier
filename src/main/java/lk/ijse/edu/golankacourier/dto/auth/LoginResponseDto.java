package lk.ijse.edu.golankacourier.dto.auth;

/**
 * --------------------------------------------
 * @Author Dimantha Kaveen
 * @GitHub: https://github.com/KaveenDK
 * --------------------------------------------
 * @Created 8/30/2025
 * @Project GoLankaCourier
 * --------------------------------------------
 **/

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response returned when a user successfully logs in.
 *
 * Note: refresh token should be sent as an HttpOnly cookie by the server.
 * The accessToken is returned here for client usage (in-memory/session).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    private String accessToken;
    private String tokenType = "Bearer";
    /**
     * Expiry in seconds (or milliseconds depending on your convention)
     * Recommend seconds here (e.g. 900 for 15 minutes).
     */
    private long expiresIn;
    private List<String> roles;
    // Optional: some apps include a non-HttpOnly refresh token; we recommend using HttpOnly cookie instead.
    // private String refreshToken;
}
