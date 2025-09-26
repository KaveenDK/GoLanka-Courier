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
import lk.ijse.edu.golankacourier.dto.parcel.ParcelSummaryDto;
import lk.ijse.edu.golankacourier.dto.parcel.ParcelDto;
import lk.ijse.edu.golankacourier.entity.Parcel;
import lk.ijse.edu.golankacourier.entity.User;
import lk.ijse.edu.golankacourier.repository.ParcelRepository;
import lk.ijse.edu.golankacourier.repository.UserRepository;
import lk.ijse.edu.golankacourier.service.ParcelService;
import lk.ijse.edu.golankacourier.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/parcels")
public class ParcelController {

    private final ParcelRepository parcelRepository;
    private final UserRepository userRepository;
    private final ParcelService parcelService;
    private final UserService userService;

    public ParcelController(ParcelRepository parcelRepository,
                            UserRepository userRepository,
                            ParcelService parcelService,
                            UserService userService) {
        this.parcelRepository = parcelRepository;
        this.userRepository = userRepository;
        this.parcelService = parcelService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<ParcelSummaryDto>> listParcels(
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit) {

        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Pageable pg = PageRequest.of(0, Math.max(1, Math.min(100, limit)));

        boolean isDriver = user.getRoles().stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_DRIVER") || r.getName().equalsIgnoreCase("DRIVER"));

        List<Parcel> parcels;
        if (isDriver) {
            parcels = parcelRepository.findByAssignedDriverId(user.getId(), pg).getContent();
        } else {
            parcels = parcelRepository.findByCustomerId(user.getId(), pg).getContent();
        }

        List<ParcelSummaryDto> results = parcels.stream()
                .map(ParcelSummaryDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getParcel(@PathVariable("id") Long id) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return parcelRepository.findById(id)
                .map(parcel -> {
                    boolean allowed =
                            (parcel.getCustomer() != null && parcel.getCustomer().getId().equals(user.getId()))
                                    || (parcel.getAssignedDriver() != null && parcel.getAssignedDriver().getId().equals(user.getId()))
                                    || user.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN") || r.getName().equalsIgnoreCase("ADMIN"));

                    if (!allowed) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                    }
                    return ResponseEntity.ok(parcel);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    public ResponseEntity<?> createParcel(@AuthenticationPrincipal UserDetails ud,
                                          @Valid @RequestBody ParcelCreateDto dto) {
        if (ud == null || ud.getUsername() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userService.findByEmail(ud.getUsername());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Parcel created = parcelService.createParcel(user.getId(), dto);
        ParcelDto out = parcelService.toDto(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(out);
    }
}
