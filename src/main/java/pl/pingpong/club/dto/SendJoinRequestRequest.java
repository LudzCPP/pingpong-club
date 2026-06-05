package pl.pingpong.club.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendJoinRequestRequest(
        @NotBlank @Email String playerEmail
) {}
