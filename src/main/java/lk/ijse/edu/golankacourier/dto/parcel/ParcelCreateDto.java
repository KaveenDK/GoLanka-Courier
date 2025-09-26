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

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParcelCreateDto {

    @NotBlank(message = "Pickup address is required")
    @Size(max = 1000)
    private String pickupAddress;

    @NotBlank(message = "Delivery address is required")
    @Size(max = 1000)
    private String deliveryAddress;

    @DecimalMin(value = "0.0", inclusive = true, message = "Weight must be non-negative")
    private BigDecimal weight;

    @Size(max = 100)
    private String dimensions;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price estimate must be positive")
    private BigDecimal priceEstimate;

    @Size(max = 2000)
    private String notes;
}

