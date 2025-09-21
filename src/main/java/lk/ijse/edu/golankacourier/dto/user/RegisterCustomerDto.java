package lk.ijse.edu.golankacourier.dto.user;

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

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for customer registration.
 * Note: server-side should validate that password and confirmPassword match.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterCustomerDto {

    @NotBlank(message = "Full name is required")
    @Size(max = 200, message = "Full name must be at most 200 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 200)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(max = 50)
    // simple phone pattern allowing digits, spaces, + and - . Adjust as needed for Sri Lanka numbers.
    @Pattern(regexp = "^[0-9+\\-\\s]{6,50}$", message = "Invalid phone number")
    private String phone;

    @NotBlank(message = "Address is required")
    @Size(max = 1000, message = "Address is too long")
    private String address;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Confirm password is required")
    @Size(min = 6, max = 100)
    private String confirmPassword;
}
