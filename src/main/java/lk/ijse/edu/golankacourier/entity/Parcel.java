package lk.ijse.edu.golankacourier.entity;

/**
 * --------------------------------------------
 * @Author Dimantha Kaveen
 * @GitHub: https://github.com/KaveenDK
 * --------------------------------------------
 * @Created 8/30/2025
 * @Project GoLankaCourier
 * --------------------------------------------
 **/

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "parcels", indexes = {
        @Index(name = "idx_parcel_tracking_code", columnList = "tracking_code")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parcel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_code", unique = true, nullable = false, length = 100)
    private String trackingCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_driver_id")
    private User assignedDriver;

    // Pickup info
    @Column(name = "pickup_address", length = 1000)
    private String pickupAddress;

    @Column(name = "pickup_latitude")
    private Double pickupLatitude;

    @Column(name = "pickup_longitude")
    private Double pickupLongitude;

    // Delivery info
    @Column(name = "delivery_address", length = 1000)
    private String deliveryAddress;

    @Column(name = "delivery_latitude")
    private Double deliveryLatitude;

    @Column(name = "delivery_longitude")
    private Double deliveryLongitude;

    // Parcel details
    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 10, scale = 2)
    private BigDecimal weight;

    /**
     * Human-friendly dimensions string like "30x20x10 cm".
     */
    @Column(length = 200)
    private String dimensions; // <-- this field fixes your builder error

    @Column(length = 2000)
    private String notes;

    @Column(length = 50)
    private String status;

    // Status history
    @OneToMany(mappedBy = "parcel", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ParcelStatusHistory> statusHistory = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
