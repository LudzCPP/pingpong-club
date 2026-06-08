package pl.pingpong.club.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.pingpong.club.dto.AdminStatsResponse;
import pl.pingpong.club.model.Role;
import pl.pingpong.club.model.TrainingStatus;
import pl.pingpong.club.model.User;
import pl.pingpong.club.repository.TrainingRepository;
import pl.pingpong.club.repository.UserRepository;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TrainingRepository trainingRepository;

    @InjectMocks private AdminService adminService;

    @Test
    void getStats_returnsAggregatedCountsAndCoachList() {
        User coach = coachWithPlayers(2);
        given(userRepository.findByRoleWithPlayers(Role.COACH)).willReturn(List.of(coach));
        given(userRepository.countByRole(Role.COACH)).willReturn(1L);
        given(userRepository.countByRoleAndActive(Role.COACH, true)).willReturn(1L);
        given(userRepository.countByRole(Role.PLAYER)).willReturn(5L);
        given(userRepository.countByRoleAndActive(Role.PLAYER, true)).willReturn(4L);
        given(trainingRepository.countByStatus(TrainingStatus.SCHEDULED)).willReturn(3L);
        given(trainingRepository.countByStatus(TrainingStatus.COMPLETED)).willReturn(10L);
        given(trainingRepository.countByStatus(TrainingStatus.CANCELLED)).willReturn(2L);
        given(trainingRepository.sumTotalPriceByStatus(TrainingStatus.COMPLETED))
                .willReturn(BigDecimal.valueOf(1500));

        AdminStatsResponse stats = adminService.getStats();

        assertThat(stats.totalCoaches()).isEqualTo(1);
        assertThat(stats.activeCoaches()).isEqualTo(1);
        assertThat(stats.totalPlayers()).isEqualTo(5);
        assertThat(stats.activePlayers()).isEqualTo(4);
        assertThat(stats.scheduledTrainings()).isEqualTo(3);
        assertThat(stats.completedTrainings()).isEqualTo(10);
        assertThat(stats.cancelledTrainings()).isEqualTo(2);
        assertThat(stats.totalEarnings()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(stats.coachStats()).hasSize(1);
        assertThat(stats.coachStats().get(0).playerCount()).isEqualTo(2);
        assertThat(stats.coachStats().get(0).email()).isEqualTo("coach@test.pl");
    }

    @Test
    void getStats_noCoaches_returnsEmptyCoachListAndZeroEarnings() {
        given(userRepository.findByRoleWithPlayers(Role.COACH)).willReturn(List.of());
        given(userRepository.countByRole(Role.COACH)).willReturn(0L);
        given(userRepository.countByRoleAndActive(Role.COACH, true)).willReturn(0L);
        given(userRepository.countByRole(Role.PLAYER)).willReturn(0L);
        given(userRepository.countByRoleAndActive(Role.PLAYER, true)).willReturn(0L);
        given(trainingRepository.countByStatus(TrainingStatus.SCHEDULED)).willReturn(0L);
        given(trainingRepository.countByStatus(TrainingStatus.COMPLETED)).willReturn(0L);
        given(trainingRepository.countByStatus(TrainingStatus.CANCELLED)).willReturn(0L);
        given(trainingRepository.sumTotalPriceByStatus(TrainingStatus.COMPLETED))
                .willReturn(BigDecimal.ZERO);

        AdminStatsResponse stats = adminService.getStats();

        assertThat(stats.coachStats()).isEmpty();
        assertThat(stats.totalEarnings()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.totalCoaches()).isZero();
    }

    @Test
    void getStats_coachWithNoPlayers_playerCountIsZero() {
        User coach = coachWithPlayers(0);
        given(userRepository.findByRoleWithPlayers(Role.COACH)).willReturn(List.of(coach));
        given(userRepository.countByRole(Role.COACH)).willReturn(1L);
        given(userRepository.countByRoleAndActive(Role.COACH, true)).willReturn(1L);
        given(userRepository.countByRole(Role.PLAYER)).willReturn(0L);
        given(userRepository.countByRoleAndActive(Role.PLAYER, true)).willReturn(0L);
        given(trainingRepository.countByStatus(TrainingStatus.SCHEDULED)).willReturn(0L);
        given(trainingRepository.countByStatus(TrainingStatus.COMPLETED)).willReturn(0L);
        given(trainingRepository.countByStatus(TrainingStatus.CANCELLED)).willReturn(0L);
        given(trainingRepository.sumTotalPriceByStatus(TrainingStatus.COMPLETED))
                .willReturn(BigDecimal.ZERO);

        AdminStatsResponse stats = adminService.getStats();

        assertThat(stats.coachStats()).hasSize(1);
        assertThat(stats.coachStats().get(0).playerCount()).isZero();
    }

    private User coachWithPlayers(int playerCount) {
        Set<User> players = new HashSet<>();
        for (int i = 0; i < playerCount; i++) {
            players.add(User.builder()
                    .id(UUID.randomUUID()).firstName("Player" + i).lastName("X")
                    .email("p" + i + "@test.pl").password("x").role(Role.PLAYER)
                    .build());
        }
        User coach = User.builder()
                .id(UUID.randomUUID()).firstName("Piotr").lastName("Trener")
                .email("coach@test.pl").password("x").role(Role.COACH)
                .build();
        coach.setPlayers(players);
        return coach;
    }
}
