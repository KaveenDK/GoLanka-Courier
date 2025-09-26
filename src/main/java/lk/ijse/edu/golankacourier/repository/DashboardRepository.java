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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DashboardRepository extends JpaRepository<Parcel, Long> {

    long countByCustomerIdAndStatus(Long customerId, String status);

    long countByCustomerIdAndStatusIn(Long customerId, List<String> statuses);

    @Query("select count(p) from Parcel p where p.customer.id = :customerId and p.scheduledPickupAt is not null and p.scheduledPickupAt > CURRENT_TIMESTAMP")
    long countScheduledPickupsForCustomer(@Param("customerId") Long customerId);

    @Query("select avg(r.rating) from Rating r where r.parcel.customer.id = :customerId")
    Double getAverageRatingForCustomer(@Param("customerId") Long customerId);

    @Query("select p from Parcel p where p.customer.id = :customerId order by p.updatedAt desc")
    List<Parcel> findTopByCustomerIdOrderByUpdatedAtDesc(@Param("customerId") Long customerId, Pageable pageable);

    @Query("select p from Parcel p where p.customer.id = :customerId and p.status = :status order by p.updatedAt desc")
    List<Parcel> findTopByCustomerIdAndStatusOrderByUpdatedAtDesc(@Param("customerId") Long customerId, @Param("status") String status, Pageable pageable);

    default List<Parcel> findTopByCustomerIdOrderByUpdatedAtDesc(Long customerId, int limit) {
        return findTopByCustomerIdOrderByUpdatedAtDesc(customerId, PageRequest.of(0, Math.max(1, limit)));
    }

    default List<Parcel> findTopByCustomerIdAndStatusOrderByUpdatedAtDesc(Long customerId, String status, int limit) {
        return findTopByCustomerIdAndStatusOrderByUpdatedAtDesc(customerId, status, PageRequest.of(0, Math.max(1, limit)));
    }
}
