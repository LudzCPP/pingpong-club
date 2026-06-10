package pl.pingpong.club.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pingpong.club.dto.TrainingPackageRequest;
import pl.pingpong.club.dto.TrainingPackageResponse;
import pl.pingpong.club.exception.BusinessRuleException;
import pl.pingpong.club.exception.ResourceNotFoundException;
import pl.pingpong.club.mapper.TrainingPackageMapper;
import pl.pingpong.club.model.Role;
import pl.pingpong.club.model.TrainingPackage;
import pl.pingpong.club.model.User;
import pl.pingpong.club.repository.TrainingPackageRepository;
import pl.pingpong.club.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainingPackageService {

    private final TrainingPackageRepository packageRepository;
    private final UserRepository            userRepository;
    private final TrainingPackageMapper     packageMapper;

    @Transactional
    public TrainingPackageResponse createPackage(TrainingPackageRequest request, String coachEmail) {
        User coach  = findByEmail(coachEmail);
        User player = findPlayerById(request.playerId(), coach);

        TrainingPackage pkg = TrainingPackage.builder()
                .player(player)
                .coach(coach)
                .totalSessions(request.totalSessions())
                .remainingSessions(request.totalSessions())
                .pricePaid(request.pricePaid())
                .notes(request.notes())
                .build();

        return packageMapper.toResponse(packageRepository.save(pkg));
    }

    /** Wszystkie pakiety dla danego gracza (tylko pakiety zalogowanego trenera). */
    public List<TrainingPackageResponse> getPackagesForPlayer(UUID playerId, String coachEmail) {
        User coach = findByEmail(coachEmail);
        return packageRepository.findAllByPlayerIdAndCoachIdOrderByCreatedAtDesc(playerId, coach.getId())
                .stream().map(packageMapper::toResponse).toList();
    }

    /** Wszystkie pakiety trenera (pogrupowane po graczu przez frontend). */
    public List<TrainingPackageResponse> getAllPackagesForCoach(String coachEmail) {
        User coach = findByEmail(coachEmail);
        return packageRepository.findAllByCoachIdOrderByCreatedAtDesc(coach.getId())
                .stream().map(packageMapper::toResponse).toList();
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
}
