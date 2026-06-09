package pl.pingpong.club.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pl.pingpong.club.dto.AuthResponse;
import pl.pingpong.club.dto.ForgotPasswordRequest;
import pl.pingpong.club.dto.InviteResponse;
import pl.pingpong.club.dto.LoginRequest;
import pl.pingpong.club.dto.RefreshRequest;
import pl.pingpong.club.dto.RegisterRequest;
import pl.pingpong.club.dto.ResetPasswordRequest;
import pl.pingpong.club.service.AuthService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/invite")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COACH')")
    public InviteResponse generateInvite(@AuthenticationPrincipal UserDetails caller) {
        return authService.generateInvite(caller.getUsername());
    }

    @PostMapping("/invite/coaches")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public InviteResponse generateCoachInvite(@AuthenticationPrincipal UserDetails admin) {
        return authService.generateCoachInvite(admin.getUsername());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(
            @Valid @RequestBody RegisterRequest request,
            @RequestParam String token
    ) {
        return authService.register(request, token);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
    }
}
