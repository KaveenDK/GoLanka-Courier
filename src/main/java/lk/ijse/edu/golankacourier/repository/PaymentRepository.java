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

import lk.ijse.edu.golankacourier.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * Repository for PaymentTransaction entity.
 */
@Repository
public interface PaymentRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByMerchantRef(String merchantRef);

    Optional<PaymentTransaction> findByProviderTxnId(String providerTxnId);

    List<PaymentTransaction> findByParcelId(Long parcelId);
}
