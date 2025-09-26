package lk.ijse.edu.golankacourier.config;

/**
 * --------------------------------------------
 * @Author Dimantha Kaveen
 * @GitHub: https://github.com/KaveenDK
 * --------------------------------------------
 * @Created 8/30/2025
 * @Project GoLankaCourier
 * --------------------------------------------
 **/

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {

    private Jwt jwt = new Jwt();
    private PayHere payhere = new PayHere();
    private Google google = new Google();
    private Cors cors = new Cors();

    @Data
    public static class Jwt {
        private String secret;
        private long accessTokenExpMs = 900000;
        private long refreshTokenExpMs = 2592000000L;
    }

    @Data
    public static class PayHere {
        private boolean sandbox = true;

        private String apiBaseUrl;

        private String merchantId;
        private String merchantSecret;
        private String webhookSecret;
        private String notifyUrl;
        private String returnUrl;
    }

    @Data
    public static class Google {
        private String mapsApiKey;
        private String oauthClientId;
        private String oauthClientSecret;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins;
    }
}

