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

/**
 * DTO returned to clients when fetching parcel details or listing parcels.
 * Contains core parcel fields and a small list of status history entries.
 */
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
    private Double pickupLatitude;
    private Double pickupLongitude;

    private String deliveryAddress;
    private Double deliveryLatitude;
    private Double deliveryLongitude;

    private Long customerId;
    private String customerName;

    private Long assignedDriverId;
    private String assignedDriverName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Status history entries in chronological order (oldest first).
     * Use the nested StatusHistoryEntry DTO for simplicity.
     */
    private List<StatusHistoryEntry> statusHistory;

    /**
     * Small nested DTO representing a single status history entry.
     * You can move this to a separate file if you prefer.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusHistoryEntry {
        private String status;
        private Long changedById;
        private String changedByName;
        private LocalDateTime timestamp;
        private Double latitude;
        private Double longitude;
        private String note;
    }
}
