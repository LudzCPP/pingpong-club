package pl.pingpong.club.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateVirtualPlayerRequest(
        @NotBlank String firstName,
        @NotBlank String lastName
) {}
