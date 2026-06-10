package pl.pingpong.club.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pingpong.club.dto.CompleteTrainingRequest;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainingService {

    private final TrainingRepository trainingRepository;
    private final UserRepository     userRepository;
    private final TrainingMapper     trainingMapper;
    private final EmailService       emailService;

    // ------------------------------------------------------------------ ZAPIS

    @Transactional
    public List<TrainingResponse> createTraining(TrainingRequest request, String coachEmail) {
        User coach = findByEmail(coachEmail);
        User player = findPlayerById(request.playerId(), coach);

        int weeks = (request.recurrenceWeeks() != null) ? request.recurrenceWeeks() : 1;
        UUID groupId = (weeks > 1) ? UUID.randomUUID() : null;

        List<Training> created = new ArrayList<>(weeks);
        for (int i = 0; i < weeks; i++) {
            LocalDateTime scheduledAt = request.scheduledAt().plusWeeks(i);
            LocalDateTime end = scheduledAt.plusMinutes(request.durationMinutes());
            if (trainingRepository.existsConflictForCoach(coach.getId(), scheduledAt, end)) {
                throw new BusinessRuleException(
                        "Trener ma już zaplanowany trening w tym czasie: " + scheduledAt + " – " + end);
            }
            Training training = Training.builder()
                    .name("trening " + player.getFirstName())
                    .player(player)
                    .coach(coach)
                    .scheduledAt(scheduledAt)
                    .durationMinutes(request.durationMinutes())
                    .totalPrice(request.totalPrice())
                    .notes(request.notes())
                    .location(request.location())
                    .recurringGroupId(groupId)
                    .build();
            created.add(trainingRepository.save(training));
        }

        emailService.sendTrainingConfirmation(created.get(0));
        return created.stream().map(trainingMapper::toResponse).toList();
    }

    @Transactional
    public TrainingResponse updateTraining(UUID id, TrainingRequest request, String coachEmail) {
        Training training = findById(id);
        assertEditable(training);

        User coach = findByEmail(coachEmail);
        User player = findPlayerById(request.playerId(), coach);

        LocalDateTime end = request.scheduledAt().plusMinutes(request.durationMinutes());

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
        training.setTotalPrice(request.totalPrice());
        training.setNotes(request.notes());
        training.setLocation(request.location());

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
    public void cancelTrainingGroup(UUID groupId) {
        List<Training> group = trainingRepository.findAllByRecurringGroupIdOrderByScheduledAt(groupId);
        if (group.isEmpty()) {
            throw new ResourceNotFoundException("Nie znaleziono cyklu treningów o ID: " + groupId);
        }
        group.stream()
                .filter(t -> t.getStatus() == TrainingStatus.SCHEDULED)
                .forEach(t -> t.setStatus(TrainingStatus.CANCELLED));
        trainingRepository.saveAll(group);
    }

    @Transactional
    public TrainingResponse completeTraining(UUID id, CompleteTrainingRequest request) {
        Training training = findById(id);
        if (training.getStatus() != TrainingStatus.SCHEDULED) {
            throw new BusinessRuleException("Tylko zaplanowany trening może być oznaczony jako zrealizowany");
        }
        training.setStatus(TrainingStatus.COMPLETED);
        if (request != null && request.notes() != null && !request.notes().isBlank()) {
            training.setNotes(request.notes());
        }
        return trainingMapper.toResponse(trainingRepository.save(training));
    }

    @Transactional
    public TrainingResponse markPaid(UUID id) {
        Training training = findById(id);
        if (training.getStatus() != TrainingStatus.COMPLETED) {
            throw new BusinessRuleException("Tylko zrealizowany trening może być oznaczony jako opłacony");
        }
        training.setPaid(!training.isPaid());
        return trainingMapper.toResponse(trainingRepository.save(training));
    }

    // ------------------------------------------------------------------ ODCZYT

    /** ADMIN widzi wszystkie, COACH widzi swoje, PLAYER widzi tylko swoje. */
    public List<TrainingResponse> getTrainings(String currentUserEmail) {
        User user = findByEmail(currentUserEmail);
        List<Training> trainings = switch (user.getRole()) {
            case ADMIN  -> trainingRepository.findAll();
            case COACH  -> trainingRepository.findAllByCoachId(user.getId());
            case PLAYER -> trainingRepository.findAllByPlayerId(user.getId());
        };
        return trainings.stream().map(trainingMapper::toResponse).toList();
    }

    /** ADMIN widzi każdy, COACH widzi swoje, PLAYER widzi tylko swój – 404 zamiast 403. */
    public TrainingResponse getTrainingById(UUID id, String currentUserEmail) {
        Training training = findById(id);
        User user = findByEmail(currentUserEmail);

        if (user.getRole() == Role.COACH
                && !training.getCoach().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Nie znaleziono treningu o ID: " + id);
        }
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

    private User findPlayerById(UUID playerId, User caller) {
        User player = userRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono zawodnika o ID: " + playerId));
        if (player.getRole() != Role.PLAYER) {
            throw new BusinessRuleException("Użytkownik " + player.getEmail() + " nie jest zawodnikiem");
        }
        if (caller.getRole() == Role.COACH
                && !userRepository.existsCoachPlayerLink(caller.getId(), playerId)) {
            throw new ResourceNotFoundException("Nie znaleziono zawodnika o ID: " + playerId);
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
