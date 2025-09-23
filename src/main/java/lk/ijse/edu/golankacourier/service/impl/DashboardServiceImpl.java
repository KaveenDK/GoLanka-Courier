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

import lk.ijse.edu.golankacourier.dto.dashboard.*;
import lk.ijse.edu.golankacourier.dto.notification.NotificationDto;
import lk.ijse.edu.golankacourier.entity.Parcel;
import lk.ijse.edu.golankacourier.entity.User;
import lk.ijse.edu.golankacourier.repository.ParcelRepository;
import lk.ijse.edu.golankacourier.service.DashboardService;
import lk.ijse.edu.golankacourier.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final ParcelRepository parcelRepository;
    private final NotificationService notificationService;

    public DashboardServiceImpl(ParcelRepository parcelRepository, NotificationService notificationService) {
        this.parcelRepository = parcelRepository;
        this.notificationService = notificationService;
    }

    @Override
    public DashboardDto getDashboardForUser(User user, int notificationLimit) {

        ProfileDto profile = ProfileDto.builder()
                .name(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress() != null ? user.getAddress().toString() : null)
                .build();

        List<Parcel> parcels = parcelRepository.findByCustomerId(user.getId(), PageRequest.of(0, 200)).getContent();

        long active = parcels.stream()
                .filter(p -> p.getStatus() != null && !p.getStatus().equalsIgnoreCase("DELIVERED") && !p.getStatus().equalsIgnoreCase("CANCELLED"))
                .count();

        long deliveredLast30 = parcels.stream()
                .filter(p -> p.getStatus() != null && p.getStatus().equalsIgnoreCase("DELIVERED"))
                .filter(p -> p.getUpdatedAt() != null && p.getUpdatedAt().isAfter(LocalDateTime.now().minusDays(30)))
                .count();

        long pendingPayments = parcels.stream().filter(p -> "PENDING_PAYMENT".equalsIgnoreCase(p.getStatus())).count();
        BigDecimal spent = parcels.stream().map(Parcel::getPrice).filter(p -> p != null).reduce(BigDecimal.ZERO, BigDecimal::add);

        MetricsDto metrics = MetricsDto.builder()
                .active((int) active)
                .delivered((int) deliveredLast30)
                .pending((int) pendingPayments)
                .spent(spent.doubleValue())
                .build();

        List<NotificationDto> notifications = notificationService.getRecentForUser(user.getId(), Math.max(1, notificationLimit));

        return DashboardDto.builder()
                .profile(profile)
                .metrics(metrics)
                .notifications(notifications)
                .build();
    }
}
