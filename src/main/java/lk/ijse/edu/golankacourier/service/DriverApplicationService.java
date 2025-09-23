package lk.ijse.edu.golankacourier.service;

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

import java.util.List;

public interface DriverApplicationService {
    DriverApplication submitApplication(DriverApplicationDto dto, String documentPath);
    List<DriverApplication> listPendingApplications();
    DriverApplication getById(Long id);
    DriverApplication approveApplication(Long id, Long reviewerId, String reviewNotes, boolean createDriverAccount);
    DriverApplication declineApplication(Long id, Long reviewerId, String reviewNotes);
}
