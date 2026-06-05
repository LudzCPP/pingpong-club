package pl.pingpong.club.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pl.pingpong.club.dto.ChangePasswordRequest;
import pl.pingpong.club.dto.CreateCoachRequest;
import pl.pingpong.club.dto.UserResponse;
import pl.pingpong.club.service.UserService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** GET /api/users/me — profil zalogowanego użytkownika (COACH i PLAYER). */
    @GetMapping("/me")
    public UserResponse getMyProfile(@AuthenticationPrincipal UserDetails user) {
        return userService.getMyProfile(user.getUsername());
    }

    /** PATCH /api/users/me/password — zmiana hasła zalogowanego użytkownika. */
    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(user.getUsername(), request);
    }

    /** GET /api/users/players — lista zawodników: COACH widzi swoich, ADMIN widzi wszystkich. */
    @GetMapping("/players")
    @PreAuthorize("hasRole('COACH')")
    public List<UserResponse> getPlayers(@AuthenticationPrincipal UserDetails caller) {
        return userService.getAllPlayers(caller.getUsername());
    }

    /** GET /api/users/coaches — lista trenerów (COACH i wyżej). */
    @GetMapping("/coaches")
    @PreAuthorize("hasRole('COACH')")
    public List<UserResponse> getCoaches() {
        return userService.getAllCoaches();
    }

    /**
     * POST /api/users/coaches — tworzenie konta trenera (tylko ADMIN).
     * Trenerzy rejestrują się przez link zaproszenia wygenerowany przez ADMIN.
     */
    @PostMapping("/coaches")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse createCoach(@Valid @RequestBody CreateCoachRequest request) {
        return userService.createCoach(request);
    }

    /** DELETE /api/users/{id} — dezaktywacja konta (tylko ADMIN). */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse deactivateUser(@PathVariable UUID id) {
        return userService.deactivateUser(id);
    }
}
