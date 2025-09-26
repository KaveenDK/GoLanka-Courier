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

import jakarta.validation.Valid;
import lk.ijse.edu.golankacourier.dto.driver.DriverApplicationDto;
import lk.ijse.edu.golankacourier.entity.DriverApplication;
import lk.ijse.edu.golankacourier.entity.Parcel;
import lk.ijse.edu.golankacourier.entity.User;
import lk.ijse.edu.golankacourier.exception.ApiException;
import lk.ijse.edu.golankacourier.repository.ParcelRepository;
import lk.ijse.edu.golankacourier.service.DriverApplicationService;
import lk.ijse.edu.golankacourier.service.ParcelService;
import lk.ijse.edu.golankacourier.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DriverController {

    private final DriverApplicationService driverApplicationService;
    private final ParcelRepository parcelRepository;
    private final UserService userService;
    private final ParcelService parcelService;

    @PostMapping(value = "/drivers/apply", consumes = {"multipart/form-data"})
    public ResponseEntity<Map<String,Object>> apply(@ModelAttribute @Valid DriverApplicationDto dto) {
        String documentPath = null;
        if (dto.getDocument() != null && !dto.getDocument().isEmpty()) {
            try {
                byte[] bytes = dto.getDocument().getBytes();
                String uploadsDir = "uploads/driver_docs";
                Files.createDirectories(Paths.get(uploadsDir));
                String filename = System.currentTimeMillis() + "-" + StringUtils.cleanPath(dto.getDocument().getOriginalFilename());
                Path path = Paths.get(uploadsDir, filename);
                Files.write(path, bytes);
                documentPath = path.toAbsolutePath().toString();
            } catch (IOException e) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save document");
            }
        }
        DriverApplication app = driverApplicationService.submitApplication(dto, documentPath);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", app.getId(), "status", app.getStatus()));
    }

    @GetMapping("/driver/parcels")
    @PreAuthorize("hasAuthority('ROLE_DRIVER')")
    public ResponseEntity<List<Parcel>> getAssignedParcels(@AuthenticationPrincipal UserDetails ud) {
        User user = userService.findByEmail(ud.getUsername());
        List<Parcel> list = parcelRepository.findByAssignedDriverId(user.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/parcels/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_DRIVER')")
    public ResponseEntity<Map<String,Object>> updateParcelStatus(@PathVariable Long id,
                                                                 @RequestBody ParcelStatusUpdateRequest req,
                                                                 @AuthenticationPrincipal UserDetails ud) {
        User user = userService.findByEmail(ud.getUsername());
        Parcel p = parcelService.updateStatus(id, req.getStatus(), user.getId(), req.getLat(), req.getLng(), req.getNote());
        return ResponseEntity.ok(Map.of("id", p.getId(), "status", p.getStatus()));
    }

    public static class ParcelStatusUpdateRequest {
        private String status;
        private Double lat;
        private Double lng;
        private String note;
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Double getLat() { return lat; }
        public void setLat(Double lat) { this.lat = lat; }
        public Double getLng() { return lng; }
        public void setLng(Double lng) { this.lng = lng; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }
}
