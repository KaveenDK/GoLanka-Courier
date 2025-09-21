package lk.ijse.edu.golankacourier.entity;

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

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions", indexes = {
        @Index(name = "idx_payment_provider_txn", columnList = "provider_txn_id"),
        @Index(name = "idx_payment_merchant_ref", columnList = "merchant_ref")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Merchant reference you set (e.g. parcel tracking or internal id)
     */
    @Column(name = "merchant_ref", length = 200)
    private String merchantRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcel_id", foreignKey = @ForeignKey(name = "fk_payment_parcel"))
    private Parcel parcel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", foreignKey = @ForeignKey(name = "fk_payment_customer"))
    private User customer;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 10)
    private String currency;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "provider_txn_id", length = 200)
    private String providerTxnId;

    /**
     * PENDING / PAID / FAILED / CANCELLED
     */
    @Column(length = 50)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Lob
    @Column(name = "raw_payload", columnDefinition = "LONGTEXT")
    private String rawPayload;
}
