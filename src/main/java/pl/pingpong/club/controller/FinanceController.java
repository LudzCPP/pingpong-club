package pl.pingpong.club.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pl.pingpong.club.dto.FinanceSummaryResponse;
import pl.pingpong.club.dto.LeagueMatchRequest;
import pl.pingpong.club.dto.LeagueMatchResponse;
import pl.pingpong.club.service.FinanceService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/finances")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    /**
     * GET /api/finances/summary?from=2024-01-01&to=2024-01-31[&playerId=uuid]
     *
     * COACH: zestawienie dla wybranego zawodnika lub wszystkich (brak playerId).
     * PLAYER: zawsze dostaje tylko własne dane (playerId ignorowany).
     */
    @GetMapping("/summary")
    public FinanceSummaryResponse getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID playerId,
            @AuthenticationPrincipal UserDetails user) {
        return financeService.getSummary(from, to, playerId, user.getUsername());
    }

    /**
     * GET /api/finances/matches
     * COACH: wszystkie mecze | PLAYER: tylko własne.
     */
    @GetMapping("/matches")
    public List<LeagueMatchResponse> getMatches(@AuthenticationPrincipal UserDetails user) {
        return financeService.getMatches(user.getUsername());
    }

    /**
     * GET /api/finances/matches/{id}
     * COACH: każdy mecz | PLAYER: tylko własny (404 dla cudzych).
     */
    @GetMapping("/matches/{id}")
    public LeagueMatchResponse getMatch(@PathVariable UUID id,
                                        @AuthenticationPrincipal UserDetails user) {
        return financeService.getMatchById(id, user.getUsername());
    }

    /**
     * POST /api/finances/matches — dodanie meczu ligowego (tylko COACH).
     */
    @PostMapping("/matches")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COACH')")
    public LeagueMatchResponse createMatch(@Valid @RequestBody LeagueMatchRequest request) {
        return financeService.createMatch(request);
    }

    /**
     * PUT /api/finances/matches/{id} — aktualizacja meczu (tylko COACH).
     */
    @PutMapping("/matches/{id}")
    @PreAuthorize("hasRole('COACH')")
    public LeagueMatchResponse updateMatch(@PathVariable UUID id,
                                           @Valid @RequestBody LeagueMatchRequest request) {
        return financeService.updateMatch(id, request);
    }

    /**
     * DELETE /api/finances/matches/{id} — usunięcie meczu (tylko COACH).
     */
    @DeleteMapping("/matches/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('COACH')")
    public void deleteMatch(@PathVariable UUID id) {
        financeService.deleteMatch(id);
    }
}
