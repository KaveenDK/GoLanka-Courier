package lk.ijse.edu.golankacourier.repository;

/**
 * --------------------------------------------
 * @Author Dimantha Kaveen
 * @GitHub: https://github.com/KaveenDK
 * --------------------------------------------
 * @Created 8/30/2025
 * @Project GoLankaCourier
 * --------------------------------------------
 **/

import lk.ijse.edu.golankacourier.entity.Parcel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParcelRepository extends JpaRepository<Parcel, Long> {

    Optional<Parcel> findByTrackingCode(String trackingCode);

    Page<Parcel> findByCustomerId(Long customerId, Pageable pageable);

    Page<Parcel> findByAssignedDriverId(Long driverId, Pageable pageable);

    Page<Parcel> findByStatus(String status, Pageable pageable);
}
