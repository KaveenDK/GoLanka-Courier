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

import lk.ijse.edu.golankacourier.dto.customer.ActivityDto;
import lk.ijse.edu.golankacourier.dto.customer.CustomerStatsDto;
import lk.ijse.edu.golankacourier.dto.customer.ParcelSummaryDto;
import lk.ijse.edu.golankacourier.dto.customer.StatusEventDto;
import lk.ijse.edu.golankacourier.dto.dashboard.DashboardDto;
import lk.ijse.edu.golankacourier.dto.dashboard.MetricsDto;
import lk.ijse.edu.golankacourier.dto.dashboard.ProfileDto;
import lk.ijse.edu.golankacourier.dto.notification.NotificationDto;
import lk.ijse.edu.golankacourier.entity.Activity;
import lk.ijse.edu.golankacourier.entity.Parcel;
import lk.ijse.edu.golankacourier.entity.ParcelStatusHistory;
import lk.ijse.edu.golankacourier.entity.User;
import lk.ijse.edu.golankacourier.repository.ActivityRepository;
import lk.ijse.edu.golankacourier.repository.ParcelRepository;
import lk.ijse.edu.golankacourier.service.DashboardService;
import lk.ijse.edu.golankacourier.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final ParcelRepository parcelRepository;
    private final NotificationService notificationService;
    private final ActivityRepository activityRepository;

    // A simple in-memory registry of SSE emitters per user id
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @Override
    public DashboardDto getDashboardForUser(User user, int notificationLimit) {

        ProfileDto profile = ProfileDto.builder()
                .name(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress() != null ? user.getAddress().toString() : null)
                .build();

        // Load a reasonable page of parcels for metrics (tweak page size if needed)
        List<Parcel> parcels = parcelRepository.findByCustomerId(user.getId(), PageRequest.of(0, 200)).getContent();

        long active = parcels.stream()
                .filter(p -> {
                    String s = p.getStatus();
                    if (s == null) return true;
                    return !s.equalsIgnoreCase("DELIVERED") && !s.equalsIgnoreCase("CANCELLED");
                })
                .count();

        long deliveredLast30 = parcels.stream()
                .filter(p -> "DELIVERED".equalsIgnoreCase(p.getStatus()))
                .filter(p -> p.getUpdatedAt() != null && p.getUpdatedAt().isAfter(LocalDateTime.now().minusDays(30)))
                .count();

        long pendingPayments = parcels.stream()
                .filter(p -> "PENDING_PAYMENT".equalsIgnoreCase(p.getStatus()))
                .count();

        BigDecimal spent = parcels.stream()
                .map(Parcel::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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


    public CustomerStatsDto getStats(Long userId) {
        long completed = parcelRepository.countByCustomerIdAndStatus(userId, "DELIVERED");
        long ongoing = parcelRepository.countByCustomerIdAndStatusIn(userId,
                List.of("IN_TRANSIT", "OUT_FOR_DELIVERY", "PICKED_UP"));

        // pickups & avgRating: call repository methods reflectively if available,
        // otherwise fall back to safe defaults (0) or simple heuristics.
        long pickups = 0;
        double avgRating = 0.0;

        // Try to call countScheduledPickupsForCustomer(Long) on repository if present
        try {
            Method m = parcelRepository.getClass().getMethod("countScheduledPickupsForCustomer", Long.class);
            Object res = m.invoke(parcelRepository, userId);
            if (res instanceof Number) pickups = ((Number) res).longValue();
        } catch (NoSuchMethodException ignored) {
            // repository method not present — attempt heuristic by scanning loaded parcels (if parcel has a scheduledPickupAt)
            try {
                // fetch a reasonable number of parcels and inspect via reflection if they expose a scheduledPickupAt getter
                List<Parcel> sample = parcelRepository.findByCustomerId(userId, PageRequest.of(0, 200)).getContent();
                if (sample != null && !sample.isEmpty()) {
                    Method getter = null;
                    try {
                        getter = sample.get(0).getClass().getMethod("getScheduledPickupAt");
                    } catch (NoSuchMethodException ex) {
                        getter = null;
                    }
                    if (getter != null) {
                        final Method finalGetter = getter;
                        pickups = sample.stream().map(p -> {
                            try {
                                Object val = finalGetter.invoke(p);
                                if (val instanceof java.time.LocalDateTime) {
                                    LocalDateTime dt = (LocalDateTime) val;
                                    return dt.isAfter(LocalDateTime.now()) ? 1L : 0L;
                                } else if (val instanceof Instant) {
                                    Instant inst = (Instant) val;
                                    return inst.isAfter(Instant.now()) ? 1L : 0L;
                                } else {
                                    return 0L;
                                }
                            } catch (Exception e) {
                                return 0L;
                            }
                        }).reduce(0L, Long::sum);
                    } else {
                        // no scheduled pickup info available
                        pickups = 0;
                    }
                } else {
                    pickups = 0;
                }
            } catch (Exception e) {
                log.warn("Unable to compute pickups heuristic, defaulting to 0", e);
                pickups = 0;
            }
        } catch (Exception ex) {
            log.warn("Error invoking countScheduledPickupsForCustomer reflectively; defaulting pickups=0", ex);
            pickups = 0;
        }

        // Try to call getAverageRatingForCustomer(Long) on repository if present
        try {
            Method m2 = parcelRepository.getClass().getMethod("getAverageRatingForCustomer", Long.class);
            Object r = m2.invoke(parcelRepository, userId);
            if (r instanceof Number) {
                avgRating = ((Number) r).doubleValue();
            } else if (r instanceof Double) {
                avgRating = (Double) r;
            }
        } catch (NoSuchMethodException ignored) {
            // repository method not present -> try heuristic: if you have a Rating entity, implement method on repo
            avgRating = 0.0;
        } catch (Exception ex) {
            log.warn("Error invoking getAverageRatingForCustomer reflectively; defaulting avgRating=0.0", ex);
            avgRating = 0.0;
        }

        return CustomerStatsDto.builder()
                .completedDeliveries(completed)
                .ongoingDeliveries(ongoing)
                .scheduledPickups(pickups)
                .averageRating(avgRating)
                .build();
    }

    public List<ParcelSummaryDto> listDeliveries(Long userId, String status, int limit) {
        List<Parcel> parcels;
        if (status == null || status.isBlank()) {
            parcels = parcelRepository.findTopByCustomerIdOrderByUpdatedAtDesc(userId, limit);
        } else {
            parcels = parcelRepository.findTopByCustomerIdAndStatusOrderByUpdatedAtDesc(userId, status, limit);
        }
        return parcels.stream().map(this::mapToParcelSummary).collect(Collectors.toList());
    }

    public List<ParcelSummaryDto> recentHistory(Long userId, int limit) {
        var list = parcelRepository.findTopDeliveredByCustomerIdOrderByDeliveredAtDesc(userId, limit);
        return list.stream().map(this::mapToParcelSummary).collect(Collectors.toList());
    }

    public List<ActivityDto> recentActivity(Long userId, int limit) {
        List<Activity> acts = activityRepository.findRecentForUser(userId, limit);
        if (acts == null || acts.isEmpty()) return Collections.emptyList();

        return acts.stream().map(a -> ActivityDto.builder()
                .id(String.valueOf(a.getId()))
                .type(a.getType())
                .title(a.getTitle())
                .description(a.getDescription())
                .timestamp(a.getTimestamp() != null ? a.getTimestamp() : Instant.now())
                .build()).collect(Collectors.toList());
    }

    public SseEmitter registerSseEmitter(Long userId) {
        final SseEmitter emitter = new SseEmitter(0L); // no timeout (or choose a sensible timeout)
        emitters.computeIfAbsent(userId, k -> Collections.synchronizedList(new ArrayList<>())).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> removeEmitter(userId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (Exception ignored) {}

        return emitter;
    }

    public void publishDeliveryUpdate(Long userId, ParcelSummaryDto update) {
        var list = emitters.get(userId);
        if (list == null) return;
        for (SseEmitter e : new ArrayList<>(list)) {
            try {
                e.send(SseEmitter.event().name("delivery-update").data(update));
            } catch (Exception ex) {
                removeEmitter(userId, e);
            }
        }
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        var list = emitters.get(userId);
        if (list != null) list.remove(emitter);
    }

    private ParcelSummaryDto mapToParcelSummary(Parcel p) {
        if (p == null) return null;

        // Use existing Parcel getters (trackingCode, status, createdAt, updatedAt, pickup/delivery address)
        ParcelSummaryDto.ParcelSummaryDtoBuilder b = ParcelSummaryDto.builder()
                .id(p.getId())
                .trackingNumber(p.getTrackingCode()) // parcel uses trackingCode field
                .status(p.getStatus())
                // default statusText to status (you can change to a human message if available)
                .statusText(p.getStatus() != null ? p.getStatus() : null)
                // we don't have a numeric progress field on Parcel entity; default to 0
                .progress(0)
                .pickupAddress(p.getPickupAddress())
                .deliveryAddress(p.getDeliveryAddress())
                .customerName(p.getCustomer() != null ? p.getCustomer().getFullName() : null);

        if (p.getCreatedAt() != null) {
            b.createdAt(p.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant());
        }
        if (p.getUpdatedAt() != null) {
            b.updatedAt(p.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant());
        }

        ParcelSummaryDto dto = b.build();

        // Map status history if present
        if (p.getStatusHistory() != null) {
            List<StatusEventDto> hist = p.getStatusHistory().stream().map(se -> {
                StatusEventDto.StatusEventDtoBuilder sb = StatusEventDto.builder()
                        .status(se.getStatus())
                        .note(se.getNote());
                if (se.getTimestamp() != null) {
                    sb.timestamp(se.getTimestamp().atZone(ZoneId.systemDefault()).toInstant());
                }
                return sb.build();
            }).collect(Collectors.toList());
            // use setter (assumes DTO has Lombok @Data / setter)
            dto.setStatusHistory(hist);
        }

        return dto;
    }
}
