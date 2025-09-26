package lk.ijse.edu.golankacourier.controller;

/**
 * --------------------------------------------
 * @Author Dimantha Kaveen
 * @GitHub: https://github.com/KaveenDK
 * --------------------------------------------
 * @Created 8/30/2025
 * @Project GoLankaCourier
 * --------------------------------------------
 **/

import lk.ijse.edu.golankacourier.dto.parcel.ParcelDto;
import lk.ijse.edu.golankacourier.entity.Parcel;
import lk.ijse.edu.golankacourier.service.ParcelService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/parcels")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://127.0.0.1:5500", "http://localhost:5500"}, allowCredentials = "true")
public class TrackingController {

    private final ParcelService parcelService;
    private final Logger log = LoggerFactory.getLogger(TrackingController.class);

    @GetMapping("/track")
    public ResponseEntity<?> trackByQuery(@RequestParam(name = "code", required = true) String code) {
        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "code query parameter is required"));
        }
        return findAndReturn(code.trim());
    }

    @GetMapping("/track/{code}")
    public ResponseEntity<?> trackByPath(@PathVariable("code") String code) {
        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "code path parameter is required"));
        }
        return findAndReturn(code.trim());
    }

    private ResponseEntity<?> findAndReturn(String code) {
        try {
            Optional<Parcel> opt = parcelService.findByTrackingCode(code);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Parcel not found"));
            }
            Parcel p = opt.get();
            ParcelDto dto = parcelService.toDto(p);
            return ResponseEntity.ok(dto);
        } catch (Exception ex) {
            log.error("Error while tracking parcel [{}]: {}", code, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Unexpected server error"));
        }
    }
}
