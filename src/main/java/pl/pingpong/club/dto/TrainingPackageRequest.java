package pl.pingpong.club.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record TrainingPackageRequest(
        @NotNull UUID playerId,
        @Positive @Max(200) int totalSessions,
        @NotNull @DecimalMin("0.00") @Digits(integer = 6, fraction = 2) BigDecimal pricePaid,
        @Size(max = 500) String notes
) {}
