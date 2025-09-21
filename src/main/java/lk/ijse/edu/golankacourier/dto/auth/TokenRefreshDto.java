package lk.ijse.edu.golankacourier.dto.auth;

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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response after calling refresh token endpoint.
 * Server typically rotates refresh token via HttpOnly cookie and returns a new access token here.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshDto {
    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn;
}

