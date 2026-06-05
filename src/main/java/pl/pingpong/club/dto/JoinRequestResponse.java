package pl.pingpong.club.dto;

import pl.pingpong.club.model.JoinRequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record JoinRequestResponse(
        UUID id,
        UUID coachId,
        String coachFirstName,
        String coachLastName,
        String coachEmail,
        JoinRequestStatus status,
        LocalDateTime createdAt
) {}
