package pl.pingpong.club.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pingpong.club.dto.TrainingRequest;
import pl.pingpong.club.dto.TrainingResponse;
import pl.pingpong.club.exception.BusinessRuleException;
import pl.pingpong.club.exception.ResourceNotFoundException;
import pl.pingpong.club.mapper.TrainingMapper;
import pl.pingpong.club.model.Role;
import pl.pingpong.club.model.Training;
import pl.pingpong.club.model.TrainingStatus;
import pl.pingpong.club.model.User;
import pl.pingpong.club.repository.TrainingRepository;
import pl.pingpong.club.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainingService {

    private final TrainingRepository trainingRepository;
    private final UserRepository userRepository;
    private final TrainingMapper trainingMapper;

    // ------------------------------------------------------------------ ZAPIS

    @Transactional
    public TrainingResponse createTraining(TrainingRequest request, String coachEmail) {
        User coach = findByEmail(coachEmail);
        User player = findPlayerById(request.playerId());

        LocalDateTime end = request.scheduledAt().plusMinutes(request.durationMinutes());
        assertNoCoachConflict(coach.getId(), request.scheduledAt(), end);

        Training training = Training.builder()
                // Bezwzględna reguła: nazwa zawsze "trening [Imię]"
                .name("trening " + player.getFirstName())
                .player(player)
                .coach(coach)
                .scheduledAt(request.scheduledAt())
                .durationMinutes(request.durationMinutes())
                .hourlyRate(request.hourlyRate())
                .notes(request.notes())
                .build();

        return trainingMapper.toResponse(trainingRepository.save(training));
    }

    @Transactional
    public TrainingResponse updateTraining(UUID id, TrainingRequest request, String coachEmail) {
        Training training = findById(id);
        assertEditable(training);

        User player = findPlayerById(request.playerId());

        LocalDateTime end = request.scheduledAt().plusMinutes(request.durationMinutes());
        // Wykluczamy edytowany trening z kontroli kolizji
        User coach = findByEmail(coachEmail);
        boolean conflict = trainingRepository.existsConflictForCoach(coach.getId(), request.scheduledAt(), end)
                && !training.getCoach().getId().equals(coach.getId());

        // Prostszy i pewniejszy check: sprawdzamy kolizje z wyłączeniem bieżącego rekordu
        if (trainingRepository.existsConflictForCoach(coach.getId(), request.scheduledAt(), end)) {
            // Kolizja ignorowana jeśli nakłada się tylko sam ze sobą
            List<Training> conflicts = trainingRepository.findAllByCoachId(coach.getId()).stream()
                    .filter(t -> !t.getId().equals(id))
                    .filter(t -> t.getStatus() != TrainingStatus.CANCELLED)
                    .filter(t -> t.getScheduledAt().isBefore(end)
                            && t.getScheduledAt().plusMinutes(t.getDurationMinutes()).isAfter(request.scheduledAt()))
                    .toList();
            if (!conflicts.isEmpty()) {
                throw new BusinessRuleException("Trener ma już zaplanowany trening w tym czasie");
            }
        }

        training.setName("trening " + player.getFirstName());
        training.setPlayer(player);
        training.setScheduledAt(request.scheduledAt());
        training.setDurationMinutes(request.durationMinutes());
        training.setHourlyRate(request.hourlyRate());
        training.setNotes(request.notes());

        return trainingMapper.toResponse(trainingRepository.save(training));
    }

    @Transactional
    public TrainingResponse cancelTraining(UUID id) {
        Training training = findById(id);
        assertEditable(training);
        training.setStatus(TrainingStatus.CANCELLED);
        return trainingMapper.toResponse(trainingRepository.save(training));
    }

    @Transactional
    public TrainingResponse completeTraining(UUID id) {
        Training training = findById(id);
        if (training.getStatus() != TrainingStatus.SCHEDULED) {
            throw new BusinessRuleException("Tylko zaplanowany trening może być oznaczony jako zrealizowany");
        }
        training.setStatus(TrainingStatus.COMPLETED);
        return trainingMapper.toResponse(trainingRepository.save(training));
    }

    // ------------------------------------------------------------------ ODCZYT

    /** COACH widzi wszystkie treningi, PLAYER widzi tylko swoje. */
    public List<TrainingResponse> getTrainings(String currentUserEmail) {
        User user = findByEmail(currentUserEmail);
        List<Training> trainings = user.getRole() == Role.COACH
                ? trainingRepository.findAll()
                : trainingRepository.findAllByPlayerId(user.getId());
        return trainings.stream().map(trainingMapper::toResponse).toList();
    }

    /** COACH widzi każdy trening, PLAYER widzi tylko swój – zwraca 404 zamiast 403. */
    public TrainingResponse getTrainingById(UUID id, String currentUserEmail) {
        Training training = findById(id);
        User user = findByEmail(currentUserEmail);

        if (user.getRole() == Role.PLAYER
                && !training.getPlayer().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Nie znaleziono treningu o ID: " + id);
        }

        return trainingMapper.toResponse(training);
    }

    // ------------------------------------------------------------------ PRYWATNE

    private Training findById(UUID id) {
        return trainingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono treningu o ID: " + id));
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika: " + email));
    }

    private User findPlayerById(UUID playerId) {
        User player = userRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono zawodnika o ID: " + playerId));
        if (player.getRole() != Role.PLAYER) {
            throw new BusinessRuleException("Użytkownik " + player.getEmail() + " nie jest zawodnikiem");
        }
        return player;
    }

    private void assertNoCoachConflict(UUID coachId, LocalDateTime start, LocalDateTime end) {
        if (trainingRepository.existsConflictForCoach(coachId, start, end)) {
            throw new BusinessRuleException("Trener ma już zaplanowany trening w tym czasie: "
                    + start + " – " + end);
        }
    }

    private void assertEditable(Training training) {
        if (training.getStatus() == TrainingStatus.COMPLETED) {
            throw new BusinessRuleException("Nie można edytować zrealizowanego treningu");
        }
    }
}
