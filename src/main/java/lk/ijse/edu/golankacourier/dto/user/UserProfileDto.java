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
 * DTO for updating or returning user profile information.
 * Email can be included but consider verifying it if user changes it (server flow).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {

    private Long id; // optional: present when returning profile

    @NotBlank(message = "Full name is required")
    @Size(max = 200)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    @Size(max = 200)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(max = 50)
    @Pattern(regexp = "^[0-9+\\-\\s]{6,50}$", message = "Invalid phone number")
    private String phone;

    @NotBlank(message = "Address is required")
    @Size(max = 1000)
    private String address;

    /**
     * Optional public URL to profile image (if you support uploads).
     */
    @Size(max = 500)
    private String profileImageUrl;
}

