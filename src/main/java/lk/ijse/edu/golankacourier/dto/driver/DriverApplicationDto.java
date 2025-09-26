package lk.ijse.edu.golankacourier.dto.driver;

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
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverApplicationDto {

    @NotBlank(message = "Full name is required")
    @Size(max = 200, message = "Full name must be at most 200 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 200)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(max = 50)
    @Pattern(regexp = "^[0-9+\\-\\s]{6,50}$", message = "Invalid phone number")
    private String phone;

    @NotBlank(message = "Address is required")
    @Size(max = 1000)
    private String address;

    @NotBlank(message = "Vehicle type is required")
    @Size(max = 100)
    private String vehicleType;

    @NotBlank(message = "NIC number is required")
    @Size(max = 50)
    private String nicNumber;

    @Size(max = 2000)
    private String description;

    private MultipartFile document;

    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;

    @Size(min = 6, max = 100)
    private String confirmPassword;
}

