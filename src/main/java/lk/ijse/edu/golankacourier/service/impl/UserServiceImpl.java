package lk.ijse.edu.golankacourier.service.impl;

/**
 * --------------------------------------------
 * @Author Dimantha Kaveen
 * @GitHub: https://github.com/KaveenDK
 * --------------------------------------------
 * @Created 8/30/2025
 * @Project GoLankaCourier
 * --------------------------------------------
 **/

import lk.ijse.edu.golankacourier.dto.user.UserProfileDto;
import lk.ijse.edu.golankacourier.entity.User;
import lk.ijse.edu.golankacourier.repository.UserRepository;
import lk.ijse.edu.golankacourier.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("User not found by email: " + email));
    }

    @Override
    public UserProfileDto getProfile(Long userId) {
        User u = findById(userId);
        return mapToProfileDto(u);
    }

    @Override
    @Transactional
    public UserProfileDto updateProfile(Long userId, UserProfileDto dto) {
        User u = findById(userId);
        u.setFullName(dto.getFullName());
        u.setPhone(dto.getPhone());
        // If you are using embedded Address, map dto.address to Address
        u.setEmail(dto.getEmail().toLowerCase(Locale.ROOT));
        // optional: profile image url
        userRepository.save(u);
        return mapToProfileDto(u);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User u = findById(userId);
        if (!passwordEncoder.matches(oldPassword, u.getPasswordHash())) {
            throw new IllegalArgumentException("Old password is incorrect");
        }
        u.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(u);
        // Optionally revoke refresh tokens for this user (via TokenService)
    }

    private UserProfileDto mapToProfileDto(User u) {
        UserProfileDto dto = new UserProfileDto();
        dto.setId(u.getId());
        dto.setFullName(u.getFullName());
        dto.setEmail(u.getEmail());
        dto.setPhone(u.getPhone());
        if (u.getAddress() != null) {
            dto.setAddress(u.getAddress().getLine());
        }
        dto.setProfileImageUrl(null); // optional
        return dto;
    }
}
