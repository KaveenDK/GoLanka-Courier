package lk.ijse.edu.golankacourier.dto.payment;

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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO used by the frontend to request payment initialization for a parcel.
 * The server will use this data to create a PaymentTransaction and build the provider-specific payload
 * (for PayHere, the server will add merchant_id, return/notify URLs and signature).
 *
 * Notes:
 *  - Do NOT send merchant secrets or provider-specific credentials from the client.
 *  - Server should validate parcel ownership and price before creating the transaction.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitDto {

    @NotNull(message = "parcelId is required")
    private Long parcelId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be positive")
    private BigDecimal amount;

    /**
     * 3-letter currency code, e.g. "LKR". Server may override or validate this.
     */
    @Size(max = 10)
    private String currency = "LKR";

    /**
     * Optional description shown to the payer (e.g. "Payment for parcel GL-12345").
     */
    @Size(max = 255)
    private String description;

    /**
     * Optional customer contact info — server should verify against authenticated user when applicable.
     */
    @Size(max = 200)
    private String customerName;

    @Size(max = 200)
    private String customerEmail;

    @Size(max = 50)
    private String customerPhone;

    /**
     * Optional: client may request a return URL; server should validate and sanitize it.
     * Typically the server will use a configured return URL for the provider.
     */
    @Size(max = 1000)
    private String returnUrl;
}
