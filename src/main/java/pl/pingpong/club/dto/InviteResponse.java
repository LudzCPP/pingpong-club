package pl.pingpong.club.dto;

import java.time.LocalDateTime;

public record InviteResponse(
        String inviteUrl,
        LocalDateTime expiresAt
) {}
