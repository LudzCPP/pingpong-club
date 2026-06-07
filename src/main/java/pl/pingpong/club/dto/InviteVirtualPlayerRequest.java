package pl.pingpong.club.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteVirtualPlayerRequest(
        @NotBlank @Email String email
) {}
