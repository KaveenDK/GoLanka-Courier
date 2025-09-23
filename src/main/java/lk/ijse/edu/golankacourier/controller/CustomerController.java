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
@RequestMapping("/api/customers")   // <- changed: customer-scoped base path to avoid conflict
@RequiredArgsConstructor
public class CustomerController {

    private final ParcelService parcelService;
    private final UserService userService;
    private final ParcelRepository parcelRepository;

    /**
     * Create a parcel for the authenticated customer
     * POST /api/customers/parcels
     */
    @PostMapping("/parcels")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<ParcelDto> createParcel(@AuthenticationPrincipal UserDetails ud,
                                                  @Valid @RequestBody ParcelCreateDto dto) {
        if (ud == null || ud.getUsername() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
        }

        User user = userService.findByEmail(ud.getUsername());
        Parcel p = parcelService.createParcel(user.getId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(parcelService.toDto(p));
    }

    /**
     * Get a parcel (customer-scoped path).
     * GET /api/customers/parcels/{id}
     * Allowed for:
     *  - parcel owner (customer)
     *  - assigned driver
     *  - admin / staff
     */
    @GetMapping("/parcels/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ParcelDto> getParcel(@AuthenticationPrincipal UserDetails ud,
                                               @PathVariable Long id) {
        if (ud == null || ud.getUsername() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
        }

        Parcel p = parcelService.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Parcel not found"));

        User requester = userService.findByEmail(ud.getUsername());

        boolean allowed = requester.getRoles().stream().anyMatch(r ->
                r.getName().equals("ROLE_ADMIN") || r.getName().equals("ROLE_STAFF"))
                || (p.getCustomer() != null && p.getCustomer().getId().equals(requester.getId()))
                || (p.getAssignedDriver() != null && p.getAssignedDriver().getId().equals(requester.getId()));

        if (!allowed) throw new ApiException(HttpStatus.FORBIDDEN, "Not authorized to view parcel");

        return ResponseEntity.ok(parcelService.toDto(p));
    }

    /**
     * List customer parcels (paged).
     * GET /api/customers/parcels
     */
    @GetMapping("/parcels")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<Page<ParcelDto>> listCustomerParcels(@AuthenticationPrincipal UserDetails ud, Pageable pageable) {
        if (ud == null || ud.getUsername() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
        }

        User user = userService.findByEmail(ud.getUsername());
        Page<Parcel> page = parcelRepository.findByCustomerId(user.getId(), pageable);
        Page<ParcelDto> dtoPage = page.map(parcelService::toDto);
        return ResponseEntity.ok(dtoPage);
    }

    // NOTE: Public tracking endpoint moved out of this controller to avoid conflicts.
    // Place public tracking in ParcelController as: GET /api/parcels/track/{trackingCode}

}
