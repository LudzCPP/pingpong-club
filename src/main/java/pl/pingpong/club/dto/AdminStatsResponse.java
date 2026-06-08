package pl.pingpong.club.dto;

import java.math.BigDecimal;

public record AdminStatsResponse(
        int totalCoaches,
        int activeCoaches,
        int totalPlayers,
        int activePlayers,
        int scheduledTrainings,
        int completedTrainings,
        int cancelledTrainings,
        BigDecimal totalEarnings
) {}
