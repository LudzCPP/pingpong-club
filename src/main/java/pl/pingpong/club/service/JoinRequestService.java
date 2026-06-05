package pl.pingpong.club.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pingpong.club.dto.JoinRequestResponse;
import pl.pingpong.club.exception.BusinessRuleException;
import pl.pingpong.club.exception.ResourceNotFoundException;
import pl.pingpong.club.model.JoinRequest;
import pl.pingpong.club.model.JoinRequestStatus;
import pl.pingpong.club.model.Role;
import pl.pingpong.club.model.User;
import pl.pingpong.club.repository.JoinRequestRepository;
import pl.pingpong.club.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JoinRequestService {

    private final JoinRequestRepository joinRequestRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /** COACH wysyła zaproszenie do zawodnika po emailu. */
    @Transactional
    public JoinRequestResponse sendRequest(String coachEmail, String playerEmail) {
        User coach = findByEmail(coachEmail);
        User player = userRepository.findByEmail(playerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika: " + playerEmail));

        if (player.getRole() != Role.PLAYER) {
            throw new BusinessRuleException("Użytkownik " + playerEmail + " nie jest zawodnikiem");
        }
        if (userRepository.existsCoachPlayerLink(coach.getId(), player.getId())) {
            throw new BusinessRuleException("Zawodnik jest już przypisany do tego trenera");
        }
        if (joinRequestRepository.existsByCoachIdAndPlayerIdAndStatus(
                coach.getId(), player.getId(), JoinRequestStatus.PENDING)) {
            throw new BusinessRuleException("Zaproszenie dla tego zawodnika jest już oczekujące");
        }

        JoinRequest request = JoinRequest.builder()
                .coach(coach)
                .player(player)
                .build();

        JoinRequest saved = joinRequestRepository.save(request);
        // emailService.sendJoinRequestNotification(saved); // odkomentuj gdy limit maili nie jest problemem
        return toResponse(saved);
    }

    /** PLAYER pobiera listę oczekujących zaproszeń skierowanych do niego. */
    public List<JoinRequestResponse> getPendingRequests(String playerEmail) {
        User player = findByEmail(playerEmail);
        return joinRequestRepository
                .findAllByPlayerIdAndStatus(player.getId(), JoinRequestStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** PLAYER akceptuje zaproszenie — tworzy relację coach_player. */
    @Transactional
    public JoinRequestResponse acceptRequest(UUID requestId, String playerEmail) {
        JoinRequest request = findRequestForPlayer(requestId, playerEmail);
        request.setStatus(JoinRequestStatus.ACCEPTED);
        userRepository.addCoachPlayerLink(request.getCoach().getId(), request.getPlayer().getId());
        return toResponse(joinRequestRepository.save(request));
    }

    /** PLAYER odrzuca zaproszenie. */
    @Transactional
    public JoinRequestResponse rejectRequest(UUID requestId, String playerEmail) {
        JoinRequest request = findRequestForPlayer(requestId, playerEmail);
        request.setStatus(JoinRequestStatus.REJECTED);
        return toResponse(joinRequestRepository.save(request));
    }

    private JoinRequest findRequestForPlayer(UUID requestId, String playerEmail) {
        User player = findByEmail(playerEmail);
        JoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono zaproszenia o ID: " + requestId));

        if (!request.getPlayer().getId().equals(player.getId())) {
            throw new ResourceNotFoundException("Nie znaleziono zaproszenia o ID: " + requestId);
        }
        if (request.getStatus() != JoinRequestStatus.PENDING) {
            throw new BusinessRuleException("Zaproszenie zostało już rozpatrzone");
        }
        return request;
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika: " + email));
    }

    private JoinRequestResponse toResponse(JoinRequest r) {
        User coach = r.getCoach();
        return new JoinRequestResponse(
                r.getId(),
                coach.getId(),
                coach.getFirstName(),
                coach.getLastName(),
                coach.getEmail(),
                r.getStatus(),
                r.getCreatedAt()
        );
    }
}
