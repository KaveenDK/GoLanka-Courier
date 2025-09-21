package lk.ijse.edu.golankacourier.service;

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

import lk.ijse.edu.golankacourier.dto.user.UserProfileDto;
import lk.ijse.edu.golankacourier.entity.User;

public interface UserService {
    User findById(Long id);
    User findByEmail(String email);
    UserProfileDto getProfile(Long userId);
    UserProfileDto updateProfile(Long userId, UserProfileDto dto);
    void changePassword(Long userId, String oldPassword, String newPassword);
}
