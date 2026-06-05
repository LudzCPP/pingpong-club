package pl.pingpong.club.dto;

import jakarta.validation.constraints.Size;

public record CompleteTrainingRequest(
        @Size(max = 500) String notes
) {}
