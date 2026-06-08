package pl.pingpong.club.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pingpong.club.dto.AdminStatsResponse;
import pl.pingpong.club.dto.AdminStatsResponse.CoachStatEntry;
import pl.pingpong.club.model.Role;
import pl.pingpong.club.model.TrainingStatus;
import pl.pingpong.club.repository.TrainingRepository;
import pl.pingpong.club.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final TrainingRepository trainingRepository;

    public AdminStatsResponse getStats() {
        List<CoachStatEntry> coachStats = userRepository.findByRoleWithPlayers(Role.COACH)
                .stream()
                .map(c -> new CoachStatEntry(
                        c.getId(),
                        c.getFirstName(),
                        c.getLastName(),
                        c.getEmail(),
                        c.isActive(),
                        c.getPlayers().size()
                ))
                .toList();

        return new AdminStatsResponse(
                (int) userRepository.countByRole(Role.COACH),
                (int) userRepository.countByRoleAndActive(Role.COACH, true),
                (int) userRepository.countByRole(Role.PLAYER),
                (int) userRepository.countByRoleAndActive(Role.PLAYER, true),
                (int) trainingRepository.countByStatus(TrainingStatus.SCHEDULED),
                (int) trainingRepository.countByStatus(TrainingStatus.COMPLETED),
                (int) trainingRepository.countByStatus(TrainingStatus.CANCELLED),
                trainingRepository.sumTotalPriceByStatus(TrainingStatus.COMPLETED),
                coachStats
        );
    }
}
