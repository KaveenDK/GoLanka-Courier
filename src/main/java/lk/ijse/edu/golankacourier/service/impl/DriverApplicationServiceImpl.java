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

import lk.ijse.edu.golankacourier.dto.driver.DriverApplicationDto;
import lk.ijse.edu.golankacourier.entity.DriverApplication;
import lk.ijse.edu.golankacourier.entity.Role;
import lk.ijse.edu.golankacourier.entity.User;
import lk.ijse.edu.golankacourier.repository.DriverApplicationRepository;
import lk.ijse.edu.golankacourier.repository.RoleRepository;
import lk.ijse.edu.golankacourier.repository.UserRepository;
import lk.ijse.edu.golankacourier.service.DriverApplicationService;
import lk.ijse.edu.golankacourier.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DriverApplicationServiceImpl implements DriverApplicationService {

    private final DriverApplicationRepository driverApplicationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public DriverApplication submitApplication(DriverApplicationDto dto, String documentPath) {
        // optionally check for existing pending application
        DriverApplication app = DriverApplication.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail().toLowerCase(Locale.ROOT))
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .vehicleType(dto.getVehicleType())
                .nicNumber(dto.getNicNumber())
                .description(dto.getDescription())
                .documentPath(documentPath)
                .status("PENDING")
                .build();
        driverApplicationRepository.save(app);

        // notify staff/admin via email (optional)
        emailService.sendDriverApplicationResult(app.getEmail(), app.getFullName(), false,
                "Your application has been received and will be reviewed.");

        return app;
    }

    @Override
    public List<DriverApplication> listPendingApplications() {
        return driverApplicationRepository.findByStatusOrderBySubmittedAtDesc("PENDING");
    }

    @Override
    public DriverApplication getById(Long id) {
        return driverApplicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Driver application not found: " + id));
    }

    @Override
    @Transactional
    public DriverApplication approveApplication(Long id, Long reviewerId, String reviewNotes, boolean createDriverAccount) {
        DriverApplication app = getById(id);
        app.setStatus("APPROVED");
        app.setReviewedAt(java.time.LocalDateTime.now());
        app.setReviewedBy(reviewerId);
        app.setReviewNotes(reviewNotes);
        driverApplicationRepository.save(app);

        if (createDriverAccount) {
            // create user for driver with temporary password and send email
            String tempPassword = generateTempPassword();
            User driver = User.builder()
                    .fullName(app.getFullName())
                    .email(app.getEmail().toLowerCase(Locale.ROOT))
                    .phone(app.getPhone())
                    .passwordHash(passwordEncoder.encode(tempPassword))
                    .enabled(true)
                    .build();
            Role driverRole = roleRepository.findByName("ROLE_DRIVER")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_DRIVER").build()));
            driver.getRoles().add(driverRole);
            userRepository.save(driver);

            emailService.sendDriverApplicationResult(app.getEmail(), app.getFullName(), true,
                    "Your application has been approved. Use the temporary password sent to log in: " + tempPassword);
        } else {
            // send approval email asking driver to set password via activation link (TODO)
            emailService.sendDriverApplicationResult(app.getEmail(), app.getFullName(), true,
                    "Your application has been approved. Please follow the activation link to create your account.");
        }

        return app;
    }

    @Override
    @Transactional
    public DriverApplication declineApplication(Long id, Long reviewerId, String reviewNotes) {
        DriverApplication app = getById(id);
        app.setStatus("DECLINED");
        app.setReviewedAt(java.time.LocalDateTime.now());
        app.setReviewedBy(reviewerId);
        app.setReviewNotes(reviewNotes);
        driverApplicationRepository.save(app);

        emailService.sendDriverApplicationResult(app.getEmail(), app.getFullName(), false, reviewNotes);
        return app;
    }

    private String generateTempPassword() {
        // basic random temporary password; improve complexity for prod
        return "Drv@" + System.currentTimeMillis() % 10000;
    }
}
