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
import lk.ijse.edu.golankacourier.dto.customer.CustomerStatsDto;
import lk.ijse.edu.golankacourier.dto.customer.ParcelSummaryDto;
import lk.ijse.edu.golankacourier.dto.customer.ActivityDto;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import lk.ijse.edu.golankacourier.entity.User;

import java.util.List;

public interface DashboardService {

    // Generic dashboard for any User (used by admin / generic endpoints)
    DashboardDto getDashboardForUser(User user, int notificationLimit);

    // Customer-specific methods (moved into this same service per request)
    CustomerStatsDto getStats(Long userId);

    List<ParcelSummaryDto> listDeliveries(Long userId, String status, int limit);

    List<ParcelSummaryDto> recentHistory(Long userId, int limit);

    List<ActivityDto> recentActivity(Long userId, int limit);

    SseEmitter registerSseEmitter(Long userId);

    // method used by other parts of backend to publish events to connected clients
    void publishDeliveryUpdate(Long userId, ParcelSummaryDto update);
}
