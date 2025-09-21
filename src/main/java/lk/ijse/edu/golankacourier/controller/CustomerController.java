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

import jakarta.validation.Valid;
import lk.ijse.edu.golankacourier.dto.parcel.ParcelCreateDto;
import lk.ijse.edu.golankacourier.dto.parcel.ParcelDto;
import lk.ijse.edu.golankacourier.entity.Parcel;
import lk.ijse.edu.golankacourier.entity.User;
import lk.ijse.edu.golankacourier.exception.ApiException;
import lk.ijse.edu.golankacourier.repository.ParcelRepository;
import lk.ijse.edu.golankacourier.service.ParcelService;
import lk.ijse.edu.golankacourier.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CustomerController {

    private final ParcelService parcelService;
    private final UserService userService;
    private final ParcelRepository parcelRepository;

    @PostMapping("/parcels")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<ParcelDto> createParcel(@AuthenticationPrincipal UserDetails ud,
                                                  @Valid @RequestBody ParcelCreateDto dto) {
        User user = userService.findByEmail(ud.getUsername());
        Parcel p = parcelService.createParcel(user.getId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(parcelService.toDto(p));
    }

    @GetMapping("/parcels/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ParcelDto> getParcel(@AuthenticationPrincipal UserDetails ud,
                                               @PathVariable Long id) {
        Parcel p = parcelService.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Parcel not found"));
        User requester = userService.findByEmail(ud.getUsername());
        boolean allowed = requester.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN") || r.getName().equals("ROLE_STAFF"))
                || (p.getCustomer() != null && p.getCustomer().getId().equals(requester.getId()))
                || (p.getAssignedDriver() != null && p.getAssignedDriver().getId().equals(requester.getId()));

        if (!allowed) throw new ApiException(HttpStatus.FORBIDDEN, "Not authorized to view parcel");
        return ResponseEntity.ok(parcelService.toDto(p));
    }

    @GetMapping("/customer/parcels")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<Page<Parcel>> listCustomerParcels(@AuthenticationPrincipal UserDetails ud, Pageable pageable) {
        User user = userService.findByEmail(ud.getUsername());
        Page<Parcel> page = parcelRepository.findByCustomerId(user.getId(), pageable);
        return ResponseEntity.ok(page);
    }

    // Public tracking endpoint
    @GetMapping("/parcels/track/{trackingCode}")
    public ResponseEntity<ParcelDto> trackParcelPublic(@PathVariable String trackingCode) {
        Parcel p = parcelService.findByTrackingCode(trackingCode).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tracking code not found"));
        return ResponseEntity.ok(parcelService.toDto(p));
    }
}
