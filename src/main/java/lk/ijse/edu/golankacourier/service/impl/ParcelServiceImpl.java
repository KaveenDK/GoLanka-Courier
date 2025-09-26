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

import lk.ijse.edu.golankacourier.dto.parcel.ParcelCreateDto;
import lk.ijse.edu.golankacourier.dto.parcel.ParcelDto;
import lk.ijse.edu.golankacourier.entity.Parcel;
import lk.ijse.edu.golankacourier.entity.ParcelStatusHistory;
import lk.ijse.edu.golankacourier.entity.User;
import lk.ijse.edu.golankacourier.repository.ParcelRepository;
import lk.ijse.edu.golankacourier.repository.ParcelStatusHistoryRepository;
import lk.ijse.edu.golankacourier.repository.UserRepository;
import lk.ijse.edu.golankacourier.service.ParcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParcelServiceImpl implements ParcelService {

    private final ParcelRepository parcelRepository;
    private final UserRepository userRepository;
    private final ParcelStatusHistoryRepository statusHistoryRepository;

    @Override
    @Transactional
    public Parcel createParcel(Long customerId, ParcelCreateDto dto) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        Parcel p = Parcel.builder()
                .trackingCode(generateTrackingCode())
                .customer(customer)
                .pickupAddress(dto.getPickupAddress())
                .deliveryAddress(dto.getDeliveryAddress())
                .weight(dto.getWeight())
                .dimensions(dto.getDimensions())
                .price(dto.getPriceEstimate() != null ? dto.getPriceEstimate() : BigDecimal.ZERO)
                .status("CREATED")
                .build();

        parcelRepository.save(p);

        // add initial status
        ParcelStatusHistory h = ParcelStatusHistory.builder()
                .parcel(p)
                .status("CREATED")
                .timestamp(LocalDateTime.now())
                .note("Parcel created by customer")
                .build();
        statusHistoryRepository.save(h);

        // link history to parcel (optional: added via cascade)
        p.getStatusHistory().add(h);
        parcelRepository.save(p);
        return p;
    }

    @Override
    public ParcelDto toDto(Parcel parcel) {
        ParcelDto dto = ParcelDto.builder()
                .id(parcel.getId())
                .trackingCode(parcel.getTrackingCode())
                .status(parcel.getStatus())
                .price(parcel.getPrice())
                .weight(parcel.getWeight())
                .dimensions(parcel.getDimensions())
                .pickupAddress(parcel.getPickupAddress())
                .deliveryAddress(parcel.getDeliveryAddress())
                .createdAt(parcel.getCreatedAt())
                .updatedAt(parcel.getUpdatedAt())
                .customerId(parcel.getCustomer() != null ? parcel.getCustomer().getId() : null)
                .customerName(parcel.getCustomer() != null ? parcel.getCustomer().getFullName() : null)
                .assignedDriverId(parcel.getAssignedDriver() != null ? parcel.getAssignedDriver().getId() : null)
                .assignedDriverName(parcel.getAssignedDriver() != null ? parcel.getAssignedDriver().getFullName() : null)
                .statusHistory(parcel.getStatusHistory().stream().map(h -> ParcelDto.StatusHistoryEntry.builder()
                        .status(h.getStatus())
                        .changedById(h.getChangedBy() != null ? h.getChangedBy().getId() : null)
                        .changedByName(h.getChangedBy() != null ? h.getChangedBy().getFullName() : null)
                        .timestamp(h.getTimestamp())
                        .note(h.getNote())
                        .build()).collect(Collectors.toList()))
                .build();
        return dto;
    }

    @Override
    public Optional<Parcel> findById(Long id) {
        return parcelRepository.findById(id);
    }

    @Override
    public Optional<Parcel> findByTrackingCode(String trackingCode) {
        return parcelRepository.findByTrackingCode(trackingCode);
    }

    @Override
    @Transactional
    public Parcel assignDriver(Long parcelId, Long driverId, Long assignedById) {
        Parcel p = parcelRepository.findById(parcelId)
                .orElseThrow(() -> new IllegalArgumentException("Parcel not found: " + parcelId));
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + driverId));
        p.setAssignedDriver(driver);
        p.setStatus("ASSIGNED");
        parcelRepository.save(p);

        ParcelStatusHistory h = ParcelStatusHistory.builder()
                .parcel(p)
                .status("ASSIGNED")
                .timestamp(LocalDateTime.now())
                .changedBy(userRepository.findById(assignedById).orElse(null))
                .note("Driver assigned")
                .build();
        statusHistoryRepository.save(h);
        p.getStatusHistory().add(h);
        parcelRepository.save(p);
        return p;
    }

    @Override
    @Transactional
    public Parcel updateStatus(Long parcelId, String status, Long changedById, Double lat, Double lng, String note) {
        Parcel p = parcelRepository.findById(parcelId)
                .orElseThrow(() -> new IllegalArgumentException("Parcel not found: " + parcelId));
        p.setStatus(status);
        parcelRepository.save(p);

        ParcelStatusHistory h = ParcelStatusHistory.builder()
                .parcel(p)
                .status(status)
                .timestamp(LocalDateTime.now())
                .note(note)
                .changedBy(changedById != null ? userRepository.findById(changedById).orElse(null) : null)
                .build();
        statusHistoryRepository.save(h);
        p.getStatusHistory().add(h);
        parcelRepository.save(p);
        return p;
    }

    private String generateTrackingCode() {
        // Simple unique code, improve for production (e.g., prefix + base36 timestamp)
        return "GL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
