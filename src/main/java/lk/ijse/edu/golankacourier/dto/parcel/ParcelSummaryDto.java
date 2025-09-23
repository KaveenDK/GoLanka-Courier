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

import com.fasterxml.jackson.annotation.JsonProperty;
import lk.ijse.edu.golankacourier.entity.Parcel;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParcelSummaryDto {

    private Long id;

    // frontend expects `trackingNumber` sometimes - map trackingCode to trackingNumber
    @JsonProperty("trackingNumber")
    private String trackingNumber;

    private String recipientName;
    private String status;
    private LocalDateTime createdAt;
    private String serviceType;
    private Double weightKg;

    public static ParcelSummaryDto fromEntity(Parcel p) {
        return ParcelSummaryDto.builder()
                .id(p.getId())
                .trackingNumber(p.getTrackingCode())
                .recipientName(p.getCustomer() != null ? p.getCustomer().getFullName() : null)
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .serviceType(null) // if you have service type field, map it here
                .weightKg(p.getWeight() != null ? p.getWeight().doubleValue() : null)
                .build();
    }
}

