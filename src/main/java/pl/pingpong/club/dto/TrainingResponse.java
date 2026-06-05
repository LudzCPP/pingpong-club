package pl.pingpong.club.dto;

import pl.pingpong.club.model.TrainingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TrainingResponse(
        UUID id,
        String name,
        UUID playerId,
        String playerFullName,
        UUID coachId,
        String coachFullName,
        LocalDateTime scheduledAt,
        int durationMinutes,
        TrainingStatus status,
        BigDecimal totalPrice,
        String notes,
        boolean paid
) {}
