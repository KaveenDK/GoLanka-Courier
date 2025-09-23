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

import lk.ijse.edu.golankacourier.entity.DriverApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for DriverApplication entity.
 */
@Repository
public interface DriverApplicationRepository extends JpaRepository<DriverApplication, Long> {

    List<DriverApplication> findByStatusOrderBySubmittedAtDesc(String status);

    Optional<DriverApplication> findByEmailAndStatus(String email, String status);

    List<DriverApplication> findByEmail(String email);
}
