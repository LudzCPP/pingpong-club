package pl.pingpong.club.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LeagueMatchResponse(
        UUID id,
        UUID playerId,
        String playerFullName,
        LocalDate matchDate,
        String opponent,
        String result,
        BigDecimal payment,
        String notes
) {}
