package lk.ijse.edu.golankacourier.dto.dashboard;

/**
 * --------------------------------------------
 * @Author Dimantha Kaveen
 * @GitHub: https://github.com/KaveenDK
 * --------------------------------------------
 * @Created 8/30/2025
 * @Project GoLankaCourier
 * --------------------------------------------
 **/

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricsDto {
    private Integer active;
    private Integer delivered;
    private Integer pending;
    private Double spent;
}
