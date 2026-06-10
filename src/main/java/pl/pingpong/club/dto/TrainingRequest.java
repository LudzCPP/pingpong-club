package pl.pingpong.club.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TrainingRequest(
        @NotNull UUID playerId,

        @NotNull @Future LocalDateTime scheduledAt,

        @Positive @Max(480) int durationMinutes,

        @NotNull @DecimalMin("0.00") @Digits(integer = 6, fraction = 2) BigDecimal totalPrice,

        @Size(max = 500) String notes,

        @Size(max = 200) String location,

        /** Jeśli podane (2–12), tworzy serię cotygodniowych treningów z tym samym recurringGroupId. */
        @Min(2) @Max(12) Integer recurrenceWeeks
) {}
