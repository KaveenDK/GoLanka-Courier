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

import lk.ijse.edu.golankacourier.dto.parcel.ParcelCreateDto;
import lk.ijse.edu.golankacourier.dto.parcel.ParcelDto;
import lk.ijse.edu.golankacourier.entity.Parcel;

import java.util.Optional;

public interface ParcelService {
    Parcel createParcel(Long customerId, ParcelCreateDto dto);
    ParcelDto toDto(Parcel parcel);
    Optional<Parcel> findById(Long id);
    Optional<Parcel> findByTrackingCode(String trackingCode);
    Parcel assignDriver(Long parcelId, Long driverId, Long assignedById);
    Parcel updateStatus(Long parcelId, String status, Long changedById, Double lat, Double lng, String note);
}
