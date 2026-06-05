package pl.pingpong.club.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pl.pingpong.club.dto.JoinRequestResponse;
import pl.pingpong.club.dto.SendJoinRequestRequest;
import pl.pingpong.club.service.JoinRequestService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/join-requests")
@RequiredArgsConstructor
public class JoinRequestController {

    private final JoinRequestService joinRequestService;

    /** POST /api/join-requests — COACH wysyła zaproszenie do zawodnika. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COACH')")
    public JoinRequestResponse sendRequest(
            @Valid @RequestBody SendJoinRequestRequest request,
            @AuthenticationPrincipal UserDetails coach
    ) {
        return joinRequestService.sendRequest(coach.getUsername(), request.playerEmail());
    }

    /** GET /api/join-requests/pending — PLAYER widzi oczekujące zaproszenia. */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('PLAYER')")
    public List<JoinRequestResponse> getPending(@AuthenticationPrincipal UserDetails player) {
        return joinRequestService.getPendingRequests(player.getUsername());
    }

    /** PATCH /api/join-requests/{id}/accept — PLAYER akceptuje zaproszenie. */
    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('PLAYER')")
    public JoinRequestResponse accept(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails player
    ) {
        return joinRequestService.acceptRequest(id, player.getUsername());
    }

    /** PATCH /api/join-requests/{id}/reject — PLAYER odrzuca zaproszenie. */
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('PLAYER')")
    public JoinRequestResponse reject(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails player
    ) {
        return joinRequestService.rejectRequest(id, player.getUsername());
    }
}
