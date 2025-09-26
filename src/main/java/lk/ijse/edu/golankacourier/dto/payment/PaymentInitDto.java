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

    @Size(max = 10)
    private String currency = "LKR";

    @Size(max = 255)
    private String description;

    @Size(max = 200)
    private String customerName;

    @Size(max = 200)
    private String customerEmail;

    @Size(max = 50)
    private String customerPhone;

    @Size(max = 1000)
    private String returnUrl;
}
