package lk.ijse.edu.golankacourier.repository;

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

import lk.ijse.edu.golankacourier.entity.ParcelStatusHistory;
import lk.ijse.edu.golankacourier.entity.Parcel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ParcelStatusHistory entries.
 */
@Repository
public interface ParcelStatusHistoryRepository extends JpaRepository<ParcelStatusHistory, Long> {

    List<ParcelStatusHistory> findByParcelOrderByTimestampAsc(Parcel parcel);

    List<ParcelStatusHistory> findByParcelIdOrderByTimestampAsc(Long parcelId);
}
