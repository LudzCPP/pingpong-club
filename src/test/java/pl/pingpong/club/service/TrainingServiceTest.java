package pl.pingpong.club.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.pingpong.club.dto.TrainingRequest;
import pl.pingpong.club.dto.TrainingResponse;
import pl.pingpong.club.exception.BusinessRuleException;
import pl.pingpong.club.exception.ResourceNotFoundException;
import pl.pingpong.club.mapper.TrainingMapper;
import pl.pingpong.club.model.Role;
import pl.pingpong.club.model.Training;
import pl.pingpong.club.model.TrainingStatus;
import pl.pingpong.club.model.User;
import pl.pingpong.club.repository.TrainingPackageRepository;
import pl.pingpong.club.repository.TrainingRepository;
import pl.pingpong.club.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrainingServiceTest {

    @Mock private TrainingRepository        trainingRepository;
    @Mock private UserRepository            userRepository;
    @Mock private TrainingMapper            trainingMapper;
    @Mock private EmailService              emailService;
    @Mock private TrainingPackageRepository trainingPackageRepository;

    @InjectMocks private TrainingService trainingService;

    // ---- createTraining ----

    @Test
    void createTraining_nameAlwaysSetFromPlayerFirstName() {
        User coach = coachUser();
        User player = playerUser("Anna");
        Training saved = trainingForCoach(coach, player);

        given(userRepository.findByEmail("coach@test.pl")).willReturn(Optional.of(coach));
        given(userRepository.findById(player.getId())).willReturn(Optional.of(player));
        given(userRepository.existsCoachPlayerLink(coach.getId(), player.getId())).willReturn(true);
        given(trainingRepository.existsConflictForCoach(any(), any(), any())).willReturn(false);
        given(trainingRepository.save(any())).willReturn(saved);
        given(trainingMapper.toResponse(saved)).willReturn(mock(TrainingResponse.class));

        trainingService.createTraining(trainingRequest(player.getId()), "coach@test.pl");

        verify(trainingRepository).save(argThat(t -> "trening Anna".equals(t.getName())));
    }

    @Test
    void createTraining_coachConflict_throwsBusinessRuleException() {
        User coach = coachUser();
        User player = playerUser("Anna");

        given(userRepository.findByEmail("coach@test.pl")).willReturn(Optional.of(coach));
        given(userRepository.findById(player.getId())).willReturn(Optional.of(player));
        given(userRepository.existsCoachPlayerLink(coach.getId(), player.getId())).willReturn(true);
        given(trainingRepository.existsConflictForCoach(eq(coach.getId()), any(), any())).willReturn(true);

        assertThatThrownBy(() ->
                trainingService.createTraining(trainingRequest(player.getId()), "coach@test.pl"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Trener ma już zaplanowany trening");
    }

    @Test
    void createTraining_assignedUserIsCoach_throwsBusinessRuleException() {
        User coach = coachUser();
        User anotherCoach = User.builder().id(UUID.randomUUID()).firstName("Bob").lastName("C")
                .email("bob@test.pl").password("x").role(Role.COACH).build();

        given(userRepository.findByEmail("coach@test.pl")).willReturn(Optional.of(coach));
        given(userRepository.findById(anotherCoach.getId())).willReturn(Optional.of(anotherCoach));

        assertThatThrownBy(() ->
                trainingService.createTraining(trainingRequest(anotherCoach.getId()), "coach@test.pl"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("nie jest zawodnikiem");
    }

    // ---- cancelTraining ----

    @Test
    void cancelTraining_scheduledTraining_setsStatusCancelled() {
        UUID id = UUID.randomUUID();
        Training training = scheduledTraining();
        given(trainingRepository.findById(id)).willReturn(Optional.of(training));
        given(trainingRepository.save(training)).willReturn(training);
        given(trainingMapper.toResponse(training)).willReturn(mock(TrainingResponse.class));

        trainingService.cancelTraining(id);

        assertThat(training.getStatus()).isEqualTo(TrainingStatus.CANCELLED);
    }

    @Test
    void cancelTraining_completedTraining_throwsBusinessRuleException() {
        UUID id = UUID.randomUUID();
        given(trainingRepository.findById(id)).willReturn(Optional.of(completedTraining()));

        assertThatThrownBy(() -> trainingService.cancelTraining(id))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("edytować");
    }

    // ---- completeTraining ----

    @Test
    void completeTraining_scheduledTraining_setsStatusCompleted() {
        UUID id = UUID.randomUUID();
        Training training = scheduledTraining();
        given(trainingRepository.findById(id)).willReturn(Optional.of(training));
        given(trainingRepository.save(training)).willReturn(training);
        given(trainingMapper.toResponse(training)).willReturn(mock(TrainingResponse.class));
        given(trainingPackageRepository
                .findFirstByPlayerIdAndCoachIdAndRemainingSessionsGreaterThanOrderByCreatedAtAsc(
                        any(), any(), anyInt()))
                .willReturn(Optional.empty());

        trainingService.completeTraining(id, null);

        assertThat(training.getStatus()).isEqualTo(TrainingStatus.COMPLETED);
    }

    @Test
    void completeTraining_alreadyCompleted_throwsBusinessRuleException() {
        UUID id = UUID.randomUUID();
        given(trainingRepository.findById(id)).willReturn(Optional.of(completedTraining()));

        assertThatThrownBy(() -> trainingService.completeTraining(id, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Tylko zaplanowany trening");
    }

    @Test
    void completeTraining_cancelledTraining_throwsBusinessRuleException() {
        UUID id = UUID.randomUUID();
        Training cancelled = scheduledTraining();
        cancelled.setStatus(TrainingStatus.CANCELLED);
        given(trainingRepository.findById(id)).willReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> trainingService.completeTraining(id, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    // ---- getTrainings ----

    @Test
    void getTrainings_coachUser_returnsOwnTrainings() {
        User coach = coachUser();
        Training t = scheduledTraining();
        given(userRepository.findByEmail("coach@test.pl")).willReturn(Optional.of(coach));
        given(trainingRepository.findAllByCoachId(coach.getId())).willReturn(List.of(t));
        given(trainingMapper.toResponse(t)).willReturn(mock(TrainingResponse.class));

        List<TrainingResponse> result = trainingService.getTrainings("coach@test.pl");

        assertThat(result).hasSize(1);
        verify(trainingRepository).findAllByCoachId(coach.getId());
        verify(trainingRepository, never()).findAllByPlayerId(any());
    }

    @Test
    void getTrainings_playerUser_returnsOnlyOwnTrainings() {
        User player = playerUser("Anna");
        given(userRepository.findByEmail("player@test.pl")).willReturn(Optional.of(player));
        given(trainingRepository.findAllByPlayerId(player.getId())).willReturn(List.of());

        trainingService.getTrainings("player@test.pl");

        verify(trainingRepository).findAllByPlayerId(player.getId());
        verify(trainingRepository, never()).findAll();
    }

    // ---- getTrainingById ----

    @Test
    void getTrainingById_playerSeesOwnTraining_returnsResponse() {
        User player = playerUser("Anna");
        UUID id = UUID.randomUUID();
        Training training = trainingWithPlayer(player);
        TrainingResponse response = mock(TrainingResponse.class);

        given(trainingRepository.findById(id)).willReturn(Optional.of(training));
        given(userRepository.findByEmail("player@test.pl")).willReturn(Optional.of(player));
        given(trainingMapper.toResponse(training)).willReturn(response);

        assertThat(trainingService.getTrainingById(id, "player@test.pl")).isEqualTo(response);
    }

    @Test
    void getTrainingById_playerSeesOtherPlayersTraining_throws404() {
        User player = playerUser("Anna");
        User otherPlayer = User.builder().id(UUID.randomUUID()).firstName("Bob").lastName("X")
                .email("bob@test.pl").password("x").role(Role.PLAYER).build();
        UUID id = UUID.randomUUID();
        Training training = trainingWithPlayer(otherPlayer);

        given(trainingRepository.findById(id)).willReturn(Optional.of(training));
        given(userRepository.findByEmail("player@test.pl")).willReturn(Optional.of(player));

        assertThatThrownBy(() -> trainingService.getTrainingById(id, "player@test.pl"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getTrainingById_coachUser_seesOwnTraining() {
        User coach = coachUser();
        User player = playerUser("Anna");
        UUID id = UUID.randomUUID();
        Training training = trainingForCoach(coach, player);
        TrainingResponse response = mock(TrainingResponse.class);

        given(trainingRepository.findById(id)).willReturn(Optional.of(training));
        given(userRepository.findByEmail("coach@test.pl")).willReturn(Optional.of(coach));
        given(trainingMapper.toResponse(training)).willReturn(response);

        assertThat(trainingService.getTrainingById(id, "coach@test.pl")).isEqualTo(response);
    }

    @Test
    void getTrainingById_coachUser_cannotSeeOtherCoachsTraining() {
        User coach = coachUser();
        User otherCoach = User.builder().id(UUID.randomUUID()).firstName("Other").lastName("Coach")
                .email("other@test.pl").password("x").role(Role.COACH).build();
        User player = playerUser("Anna");
        UUID id = UUID.randomUUID();
        Training training = trainingForCoach(otherCoach, player);

        given(trainingRepository.findById(id)).willReturn(Optional.of(training));
        given(userRepository.findByEmail("coach@test.pl")).willReturn(Optional.of(coach));

        assertThatThrownBy(() -> trainingService.getTrainingById(id, "coach@test.pl"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- helpers ----

    private User coachUser() {
        return User.builder().id(UUID.randomUUID()).firstName("Piotr").lastName("Trener")
                .email("coach@test.pl").password("x").role(Role.COACH).build();
    }

    private User playerUser(String firstName) {
        return User.builder().id(UUID.randomUUID()).firstName(firstName).lastName("Kowalski")
                .email("player@test.pl").password("x").role(Role.PLAYER).build();
    }

    private TrainingRequest trainingRequest(UUID playerId) {
        return new TrainingRequest(
                playerId,
                LocalDateTime.now().plusDays(1),
                60,
                BigDecimal.valueOf(100),
                null, null, null
        );
    }

    private Training scheduledTraining() {
        return Training.builder()
                .id(UUID.randomUUID())
                .name("trening Anna")
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .durationMinutes(60)
                .totalPrice(BigDecimal.valueOf(100))
                .player(playerUser("Anna"))
                .coach(coachUser())
                .build();
    }

    private Training completedTraining() {
        Training t = scheduledTraining();
        t.setStatus(TrainingStatus.COMPLETED);
        return t;
    }

    private Training trainingWithPlayer(User player) {
        return trainingForCoach(coachUser(), player);
    }

    private Training trainingForCoach(User coach, User player) {
        return Training.builder()
                .id(UUID.randomUUID())
                .name("trening " + player.getFirstName())
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .durationMinutes(60)
                .totalPrice(BigDecimal.valueOf(100))
                .player(player)
                .coach(coach)
                .build();
    }
}
