package pl.pingpong.club.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrainingParseRequest(
        @NotBlank @Size(max = 1000) String text
) {}
