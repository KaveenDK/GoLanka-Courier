package lk.ijse.edu.golankacourier.dto.customer;

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

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerStatsDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private long completedDeliveries;
    private long ongoingDeliveries;
    private long scheduledPickups;
    private double averageRating;
}
