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
 * Generic auth response (used for signup, verify, forgot-password replies).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
    private boolean success;
    private String message;
}
