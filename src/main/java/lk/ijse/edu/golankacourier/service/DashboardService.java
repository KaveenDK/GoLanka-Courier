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

import lk.ijse.edu.golankacourier.dto.dashboard.DashboardDto;
import lk.ijse.edu.golankacourier.entity.User;

public interface DashboardService {
    DashboardDto getDashboardForUser(User user, int notificationLimit);
}
