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

    /** GET /api/users/players — lista wszystkich zawodników (tylko COACH). */
    @GetMapping("/players")
    @PreAuthorize("hasRole('COACH')")
    public List<UserResponse> getPlayers() {
        return userService.getAllPlayers();
    }

    /** GET /api/users/coaches — lista wszystkich trenerów (tylko COACH). */
    @GetMapping("/coaches")
    @PreAuthorize("hasRole('COACH')")
    public List<UserResponse> getCoaches() {
        return userService.getAllCoaches();
    }

    /**
     * POST /api/users/coaches — tworzenie konta trenera (tylko COACH).
     * Rejestracja publiczna zawsze tworzy PLAYER; COACH tworzy innych trenerów tutaj.
     */
    @PostMapping("/coaches")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COACH')")
    public UserResponse createCoach(@Valid @RequestBody CreateCoachRequest request) {
        return userService.createCoach(request);
    }

    /** DELETE /api/users/{id} — dezaktywacja konta (tylko COACH). */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COACH')")
    public UserResponse deactivateUser(@PathVariable UUID id) {
        return userService.deactivateUser(id);
    }
}
