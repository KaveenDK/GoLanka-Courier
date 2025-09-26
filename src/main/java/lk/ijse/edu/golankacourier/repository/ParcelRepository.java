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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParcelRepository extends JpaRepository<Parcel, Long> {

    Optional<Parcel> findByTrackingCode(String trackingCode);

    Page<Parcel> findByCustomerId(Long customerId, Pageable pageable);

    Page<Parcel> findByAssignedDriverId(Long driverId, Pageable pageable);

    Page<Parcel> findByStatus(String status, Pageable pageable);

    long countByCustomerIdAndStatus(Long customerId, String status);

    long countByCustomerIdAndStatusIn(Long customerId, List<String> statuses);

    @Query("select p from Parcel p where p.customer.id = :customerId order by p.updatedAt desc")
    List<Parcel> findByCustomerIdOrderByUpdatedAtDesc(@Param("customerId") Long customerId, Pageable pageable);

    @Query("select p from Parcel p where p.customer.id = :customerId and p.status = :status order by p.updatedAt desc")
    List<Parcel> findByCustomerIdAndStatusOrderByUpdatedAtDesc(@Param("customerId") Long customerId, @Param("status") String status, Pageable pageable);

    @Query("select p from Parcel p where p.customer.id = :customerId and p.status = 'DELIVERED' order by p.updatedAt desc")
    List<Parcel> findDeliveredByCustomerIdOrderByDeliveredAtDesc(@Param("customerId") Long customerId, Pageable pageable);

    default List<Parcel> findTopByCustomerIdOrderByUpdatedAtDesc(Long customerId, int limit) {
        return findByCustomerIdOrderByUpdatedAtDesc(customerId, PageRequest.of(0, Math.max(1, limit)));
    }

    default List<Parcel> findTopByCustomerIdAndStatusOrderByUpdatedAtDesc(Long customerId, String status, int limit) {
        return findByCustomerIdAndStatusOrderByUpdatedAtDesc(customerId, status, PageRequest.of(0, Math.max(1, limit)));
    }

    default List<Parcel> findTopDeliveredByCustomerIdOrderByDeliveredAtDesc(Long customerId, int limit) {
        return findDeliveredByCustomerIdOrderByDeliveredAtDesc(customerId, PageRequest.of(0, Math.max(1, limit)));
    }
}
