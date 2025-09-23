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

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reusable embeddable address. Stored inline in user or other entities.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Column(name = "address_line", columnDefinition = "TEXT")
    private String line;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String province;

    @Column(length = 50)
    private String postalCode;

    @Column(length = 100)
    private String country;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;
}
