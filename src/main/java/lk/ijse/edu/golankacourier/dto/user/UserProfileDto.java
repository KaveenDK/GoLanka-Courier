package lk.ijse.edu.golankacourier.dto.user;

/**
 * --------------------------------------------
 * @Author Dimantha Kaveen
 * @GitHub: https://github.com/KaveenDK
 * --------------------------------------------
 * @Created 8/30/2025
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

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {

    private Long id;

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

    @Size(max = 500)
    private String profileImageUrl;
}

