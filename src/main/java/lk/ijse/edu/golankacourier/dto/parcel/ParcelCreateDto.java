package lk.ijse.edu.golankacourier.dto.parcel;

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

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO used by customers to create a new parcel/shipment request.
 *
 * Notes:
 * - customer identity should usually be taken from the authenticated principal, not from the request payload.
 * - Coordinates are optional (can be filled by Google Places autocomplete on the client).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParcelCreateDto {

    @NotBlank(message = "Pickup address is required")
    @Size(max = 1000)
    private String pickupAddress;

    /**
     * Optional: latitude for pickup address (use Double to allow null).
     */
    private Double pickupLatitude;

    /**
     * Optional: longitude for pickup address.
     */
    private Double pickupLongitude;

    @NotBlank(message = "Delivery address is required")
    @Size(max = 1000)
    private String deliveryAddress;

    private Double deliveryLatitude;
    private Double deliveryLongitude;

    /**
     * Weight in kilograms. Must be >= 0.
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "Weight must be non-negative")
    private BigDecimal weight;

    /**
     * Optional human-friendly dimensions, e.g. "30x20x10 cm".
     */
    @Size(max = 100)
    private String dimensions;

    /**
     * Optional price estimate (server should compute final price in many cases).
     */
    @DecimalMin(value = "0.0", inclusive = false, message = "Price estimate must be positive")
    private BigDecimal priceEstimate;

    @Size(max = 2000)
    private String notes;
}

