package pl.pingpong.club.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.pingpong.club.dto.AdminStatsResponse;
import pl.pingpong.club.model.Role;
import pl.pingpong.club.model.TrainingStatus;
import pl.pingpong.club.repository.TrainingRepository;
import pl.pingpong.club.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final TrainingRepository trainingRepository;

    public AdminStatsResponse getStats() {
        return new AdminStatsResponse(
                (int) userRepository.countByRole(Role.COACH),
                (int) userRepository.countByRoleAndActive(Role.COACH, true),
                (int) userRepository.countByRole(Role.PLAYER),
                (int) userRepository.countByRoleAndActive(Role.PLAYER, true),
                (int) trainingRepository.countByStatus(TrainingStatus.SCHEDULED),
                (int) trainingRepository.countByStatus(TrainingStatus.COMPLETED),
                (int) trainingRepository.countByStatus(TrainingStatus.CANCELLED),
                trainingRepository.sumTotalPriceByStatus(TrainingStatus.COMPLETED)
        );
    }
}
