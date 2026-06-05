package pl.pingpong.club.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TrainingParseResponse(
        UUID playerId,
        String playerName,
        LocalDateTime scheduledAt,
        Integer durationMinutes,
        BigDecimal hourlyRate,
        String notes
) {}
