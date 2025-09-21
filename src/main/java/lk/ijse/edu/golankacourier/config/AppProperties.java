package lk.ijse.edu.golankacourier.config;

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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import java.util.List;

/**
 * AppProperties bound to "app.*"
 */
@Component
@ConfigurationProperties(prefix = "app")
// @Validated
@Data
public class AppProperties {

    private Jwt jwt = new Jwt();
    private PayHere payhere = new PayHere();
    private Google google = new Google();
    private Cors cors = new Cors();

    @Data
    public static class Jwt {
        // @NotBlank // uncomment if using @Validated
        private String secret;
        private long accessTokenExpMs = 900000;
        private long refreshTokenExpMs = 2592000000L;
    }

    @Data
    public static class PayHere {
        private boolean sandbox = true;

        /**
         * Example property keys that will map:
         * - app.payhere.api-base-url
         * - app.payhere.apiBaseUrl
         */
        private String apiBaseUrl;           // maps to app.payhere.api-base-url

        private String merchantId;          // app.payhere.merchant-id or app.payhere.merchantId
        private String merchantSecret;      // app.payhere.merchant-secret
        private String webhookSecret;       // app.payhere.webhook-secret
        private String notifyUrl;           // app.payhere.notify-url
        private String returnUrl;           // app.payhere.return-url
    }

    @Data
    public static class Google {
        private String mapsApiKey;          // app.google.maps-api-key
        private String oauthClientId;
        private String oauthClientSecret;
    }

    @Data
    public static class Cors {
        /**
         * Comma-separated allowed origins bind to List<String>
         * Example property: app.cors.allowed-origins=http://localhost:3000,http://localhost:8080
         */
        private List<String> allowedOrigins;
    }
}

