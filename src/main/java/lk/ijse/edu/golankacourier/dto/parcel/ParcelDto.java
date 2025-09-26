package lk.ijse.edu.golankacourier.dto.parcel;

/**
 * --------------------------------------------
 * @Author Dimantha Kaveen
 * @GitHub: https://github.com/KaveenDK
 * --------------------------------------------
 * @Created 8/30/2025
 * @Project GoLankaCourier
 * --------------------------------------------
 **/

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParcelDto {

    private Long id;
    private String trackingCode;
    private String status;

    private BigDecimal price;
    private BigDecimal weight;
    private String dimensions;

    private String pickupAddress;

    private String deliveryAddress;

    private Long customerId;
    private String customerName;

    private Long assignedDriverId;
    private String assignedDriverName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<StatusHistoryEntry> statusHistory;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusHistoryEntry {
        private String status;
        private Long changedById;
        private String changedByName;
        private LocalDateTime timestamp;
        private String note;
    }
}
