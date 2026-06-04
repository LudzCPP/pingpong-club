package pl.pingpong.club.dto;

import pl.pingpong.club.model.Role;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        Role role,
        boolean active
) {}
