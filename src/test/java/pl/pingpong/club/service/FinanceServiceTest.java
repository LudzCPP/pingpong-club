package pl.pingpong.club.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.pingpong.club.dto.LeagueMatchRequest;
import pl.pingpong.club.dto.LeagueMatchResponse;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    @Mock private TrainingRepository trainingRepository;
    @Mock private LeagueMatchRepository leagueMatchRepository;
    @Mock private UserRepository userRepository;
    @Mock private TrainingMapper trainingMapper;
    @Mock private LeagueMatchMapper leagueMatchMapper;

    @InjectMocks private FinanceService financeService;

    @Test
    void getSummary_callerIsPlayer_queriesOnlyOwnData() {
        User player = playerUser();
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 31);

        given(userRepository.findByEmail("player@test.pl")).willReturn(Optional.of(player));
        given(trainingRepository.findByPlayerAndPeriodAndStatus(
                eq(player.getId()), any(), any(), eq(TrainingStatus.COMPLETED)))
                .willReturn(List.of());
        given(leagueMatchRepository.findByPlayerAndPeriod(eq(player.getId()), eq(from), eq(to)))
                .willReturn(List.of());

        var summary = financeService.getSummary(from, to, null, "player@test.pl");

        assertThat(summary.completedTrainingsCount()).isZero();
        assertThat(summary.matchesCount()).isZero();
        verify(trainingRepository).findByPlayerAndPeriodAndStatus(
                eq(player.getId()), any(), any(), eq(TrainingStatus.COMPLETED));
    }

    @Test
    void getSummary_callerIsCoachWithNullPlayerId_queriesOwnTrainingsAndPlayers() {
        User coach = coachUser();
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 31);

        given(userRepository.findByEmail("coach@test.pl")).willReturn(Optional.of(coach));
        given(userRepository.findPlayersByCoachId(coach.getId())).willReturn(List.of());
        given(trainingRepository.findAllByCoachId(coach.getId())).willReturn(List.of());

        financeService.getSummary(from, to, null, "coach@test.pl");

        verify(trainingRepository).findAllByCoachId(coach.getId());
        verify(userRepository).findPlayersByCoachId(coach.getId());
    }

    @Test
    void getMatchById_playerSeesOtherPlayersMatch_throws404() {
        User player = playerUser();
        User otherPlayer = User.builder().id(UUID.randomUUID()).firstName("Bob").lastName("X")
                .email("bob@test.pl").password("x").role(Role.PLAYER).build();
        UUID matchId = UUID.randomUUID();
        LeagueMatch match = matchForPlayer(otherPlayer, matchId);

        given(leagueMatchRepository.findById(matchId)).willReturn(Optional.of(match));
        given(userRepository.findByEmail("player@test.pl")).willReturn(Optional.of(player));

        assertThatThrownBy(() -> financeService.getMatchById(matchId, "player@test.pl"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMatchById_playerSeesOwnMatch_returnsResponse() {
        User player = playerUser();
        UUID matchId = UUID.randomUUID();
        LeagueMatch match = matchForPlayer(player, matchId);
        LeagueMatchResponse response = mock(LeagueMatchResponse.class);

        given(leagueMatchRepository.findById(matchId)).willReturn(Optional.of(match));
        given(userRepository.findByEmail("player@test.pl")).willReturn(Optional.of(player));
        given(leagueMatchMapper.toResponse(match)).willReturn(response);

        assertThat(financeService.getMatchById(matchId, "player@test.pl")).isEqualTo(response);
    }

    @Test
    void createMatch_savesAndReturnsResponse() {
        User player = playerUser();
        LeagueMatchRequest request = matchRequest(player.getId());
        LeagueMatch saved = matchForPlayer(player, UUID.randomUUID());
        LeagueMatchResponse response = mock(LeagueMatchResponse.class);

        given(userRepository.findById(player.getId())).willReturn(Optional.of(player));
        given(leagueMatchRepository.save(any())).willReturn(saved);
        given(leagueMatchMapper.toResponse(saved)).willReturn(response);

        assertThat(financeService.createMatch(request)).isEqualTo(response);
        verify(leagueMatchRepository).save(any());
    }

    @Test
    void deleteMatch_callsRepositoryDelete() {
        UUID matchId = UUID.randomUUID();
        User player = playerUser();
        LeagueMatch match = matchForPlayer(player, matchId);

        given(leagueMatchRepository.findById(matchId)).willReturn(Optional.of(match));

        financeService.deleteMatch(matchId);

        verify(leagueMatchRepository).delete(match);
    }

    private User playerUser() {
        return User.builder().id(UUID.randomUUID()).firstName("Anna").lastName("Nowak")
                .email("player@test.pl").password("x").role(Role.PLAYER).build();
    }

    private User coachUser() {
        return User.builder().id(UUID.randomUUID()).firstName("Piotr").lastName("Trener")
                .email("coach@test.pl").password("x").role(Role.COACH).build();
    }

    private LeagueMatch matchForPlayer(User player, UUID id) {
        return LeagueMatch.builder()
                .id(id)
                .player(player)
                .matchDate(LocalDate.of(2024, 1, 15))
                .opponent("Club XYZ")
                .result("3:1")
                .payment(BigDecimal.valueOf(50))
                .build();
    }

    private LeagueMatchRequest matchRequest(UUID playerId) {
        return new LeagueMatchRequest(
                playerId,
                LocalDate.of(2024, 1, 15),
                "Club XYZ",
                "3:1",
                BigDecimal.valueOf(50),
                null
        );
    }
}
