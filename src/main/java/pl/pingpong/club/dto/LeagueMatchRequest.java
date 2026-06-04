package pl.pingpong.club.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LeagueMatchRequest(
        @NotNull UUID playerId,
        @NotNull @PastOrPresent LocalDate matchDate,
        @NotBlank @Size(max = 200) String opponent,
        @NotBlank @Size(max = 10) @Pattern(regexp = "\\d+:\\d+", message = "Wynik musi być w formacie X:Y, np. 3:1") String result,
        @NotNull @DecimalMin("0.00") @Digits(integer = 6, fraction = 2) BigDecimal payment,
        @Size(max = 500) String notes
) {}
