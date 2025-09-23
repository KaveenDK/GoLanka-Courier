package lk.ijse.edu.golankacourier.dto.dashboard;

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
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileDto {
    private String name;
    private String email;
    private String phone;
    private String address;

    public static ProfileDto fromUser(User u) {
        return ProfileDto.builder()
                .name(u.getFullName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .address(u.getAddress() != null ? u.getAddress().toString() : null)
                .build();
    }
}
