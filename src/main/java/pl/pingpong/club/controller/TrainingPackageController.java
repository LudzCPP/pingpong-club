package pl.pingpong.club.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pl.pingpong.club.dto.TrainingPackageRequest;
import pl.pingpong.club.dto.TrainingPackageResponse;
import pl.pingpong.club.service.TrainingPackageService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/packages")
@RequiredArgsConstructor
public class TrainingPackageController {

    private final TrainingPackageService packageService;

    /** GET /api/packages — wszystkie pakiety trenera */
    @GetMapping
    @PreAuthorize("hasRole('COACH')")
    public List<TrainingPackageResponse> getAllPackages(@AuthenticationPrincipal UserDetails user) {
        return packageService.getAllPackagesForCoach(user.getUsername());
    }

    /** GET /api/packages?playerId=X — pakiety dla konkretnego zawodnika */
    @GetMapping(params = "playerId")
    @PreAuthorize("hasRole('COACH')")
    public List<TrainingPackageResponse> getPackagesForPlayer(
            @RequestParam UUID playerId,
            @AuthenticationPrincipal UserDetails user) {
        return packageService.getPackagesForPlayer(playerId, user.getUsername());
    }

    /** POST /api/packages — COACH rejestruje nowy pakiet dla zawodnika */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COACH')")
    public TrainingPackageResponse createPackage(
            @Valid @RequestBody TrainingPackageRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return packageService.createPackage(request, user.getUsername());
    }
}
