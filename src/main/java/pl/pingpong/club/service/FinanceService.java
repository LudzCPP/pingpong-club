package pl.pingpong.club.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pingpong.club.dto.FinanceSummaryResponse;
import pl.pingpong.club.dto.LeagueMatchRequest;
import pl.pingpong.club.dto.LeagueMatchResponse;
import pl.pingpong.club.dto.TrainingResponse;
import pl.pingpong.club.exception.ResourceNotFoundException;
import pl.pingpong.club.mapper.LeagueMatchMapper;
import pl.pingpong.club.mapper.TrainingMapper;
import pl.pingpong.club.model.LeagueMatch;
import pl.pingpong.club.model.Role;
import pl.pingpong.club.model.TrainingStatus;
import pl.pingpong.club.model.User;
import pl.pingpong.club.repository.LeagueMatchRepository;
import pl.pingpong.club.repository.TrainingRepository;
import pl.pingpong.club.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinanceService {

    private final TrainingRepository trainingRepository;
    private final LeagueMatchRepository leagueMatchRepository;
    private final UserRepository userRepository;
    private final TrainingMapper trainingMapper;
    private final LeagueMatchMapper leagueMatchMapper;

    /**
     * Zestawienie finansowe za podany przedział dat.
     * COACH: może zapytać o dowolnego zawodnika (playerId) lub o wszystkich (playerId = null).
     * PLAYER: zawsze dostaje tylko własne dane, parametr playerId ignorowany.
     */
    public FinanceSummaryResponse getSummary(LocalDate from, LocalDate to,
                                              UUID playerId, String currentUserEmail) {
        User caller = findByEmail(currentUserEmail);

        // PLAYER widzi tylko siebie
        UUID resolvedPlayerId = caller.getRole() == Role.PLAYER ? caller.getId() : playerId;

        List<TrainingResponse> trainings = resolvedPlayerId != null
                ? trainingRepository.findByPlayerAndPeriodAndStatus(
                        resolvedPlayerId,
                        from.atStartOfDay(),
                        to.atTime(LocalTime.MAX),
                        TrainingStatus.COMPLETED)
                  .stream().map(trainingMapper::toResponse).toList()
                : trainingRepository.findAllByStatus(TrainingStatus.COMPLETED).stream()
                  .filter(t -> !t.getScheduledAt().toLocalDate().isBefore(from)
                          && !t.getScheduledAt().toLocalDate().isAfter(to))
                  .map(trainingMapper::toResponse).toList();

        List<LeagueMatchResponse> matches = resolvedPlayerId != null
                ? leagueMatchRepository.findByPlayerAndPeriod(resolvedPlayerId, from, to)
                  .stream().map(leagueMatchMapper::toResponse).toList()
                : leagueMatchRepository.findAllByPeriod(from, to)
                  .stream().map(leagueMatchMapper::toResponse).toList();

        BigDecimal trainingEarnings = trainings.stream()
                .map(TrainingResponse::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal matchPayments = matches.stream()
                .map(LeagueMatchResponse::payment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new FinanceSummaryResponse(
                from, to,
                trainingEarnings,
                matchPayments,
                trainingEarnings.add(matchPayments),
                trainings.size(),
                matches.size(),
                trainings,
                matches
        );
    }

    // ------------------------------------------------------------------ MECZE

    public List<LeagueMatchResponse> getMatches(String currentUserEmail) {
        User caller = findByEmail(currentUserEmail);
        List<LeagueMatch> matches = caller.getRole() == Role.PLAYER
                ? leagueMatchRepository.findAllByPlayerIdOrderByMatchDateDesc(caller.getId())
                : leagueMatchRepository.findAll();
        return matches.stream().map(leagueMatchMapper::toResponse).toList();
    }

    public LeagueMatchResponse getMatchById(UUID id, String currentUserEmail) {
        LeagueMatch match = findMatchById(id);
        User caller = findByEmail(currentUserEmail);
        if (caller.getRole() == Role.PLAYER && !match.getPlayer().getId().equals(caller.getId())) {
            throw new ResourceNotFoundException("Nie znaleziono meczu o ID: " + id);
        }
        return leagueMatchMapper.toResponse(match);
    }

    @Transactional
    public LeagueMatchResponse createMatch(LeagueMatchRequest request) {
        User player = userRepository.findById(request.playerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nie znaleziono zawodnika o ID: " + request.playerId()));

        LeagueMatch match = LeagueMatch.builder()
                .player(player)
                .matchDate(request.matchDate())
                .opponent(request.opponent())
                .result(request.result())
                .payment(request.payment())
                .notes(request.notes())
                .build();

        return leagueMatchMapper.toResponse(leagueMatchRepository.save(match));
    }

    @Transactional
    public LeagueMatchResponse updateMatch(UUID id, LeagueMatchRequest request) {
        LeagueMatch match = findMatchById(id);
        User player = userRepository.findById(request.playerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nie znaleziono zawodnika o ID: " + request.playerId()));

        match.setPlayer(player);
        match.setMatchDate(request.matchDate());
        match.setOpponent(request.opponent());
        match.setResult(request.result());
        match.setPayment(request.payment());
        match.setNotes(request.notes());

        return leagueMatchMapper.toResponse(leagueMatchRepository.save(match));
    }

    @Transactional
    public void deleteMatch(UUID id) {
        leagueMatchRepository.delete(findMatchById(id));
    }

    // ------------------------------------------------------------------ PRYWATNE

    private LeagueMatch findMatchById(UUID id) {
        return leagueMatchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono meczu o ID: " + id));
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika: " + email));
    }
}
