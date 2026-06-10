package pl.pingpong.club.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TrainingPackageResponse(
        UUID id,
        UUID playerId,
        String playerFullName,
        UUID coachId,
        String coachFullName,
        int totalSessions,
        int remainingSessions,
        BigDecimal pricePaid,
        String notes,
        LocalDateTime createdAt
) {}
