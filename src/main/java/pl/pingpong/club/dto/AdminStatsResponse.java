package pl.pingpong.club.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AdminStatsResponse(
        int totalCoaches,
        int activeCoaches,
        int totalPlayers,
        int activePlayers,
        int scheduledTrainings,
        int completedTrainings,
        int cancelledTrainings,
        BigDecimal totalEarnings,
        List<CoachStatEntry> coachStats
) {
    public record CoachStatEntry(
            UUID id,
            String firstName,
            String lastName,
            String email,
            boolean active,
            int playerCount
    ) {}
}
