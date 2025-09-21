package lk.ijse.edu.golankacourier.controller;

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

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/maps")
@RequiredArgsConstructor
public class MapController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${google.maps.api-key:}")
    private String googleApiKey;

    /**
     * Proxy for Google Geocoding API to avoid exposing server key for server-side geocoding.
     * For client-side maps (Maps JS), use a browser-restricted API key and call Google directly.
     */
    @GetMapping("/geocode")
    public ResponseEntity<String> geocode(@RequestParam String address) {
        if (googleApiKey == null || googleApiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Google Maps API key not configured");
        }
        String url = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
                .queryParam("address", address)
                .queryParam("key", googleApiKey)
                .toUriString();

        ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
        return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
    }
}
