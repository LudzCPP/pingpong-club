package pl.pingpong.club.dto;

import pl.pingpong.club.model.Role;

import java.time.Instant;

public record AuthResponse(
        String token,
        String refreshToken,
        String email,
        Role role,
        Instant expiresAt,
        String firstName,
        String lastName
) {}
