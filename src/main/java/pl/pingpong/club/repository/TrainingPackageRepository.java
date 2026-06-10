package pl.pingpong.club.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pingpong.club.model.TrainingPackage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingPackageRepository extends JpaRepository<TrainingPackage, UUID> {

    List<TrainingPackage> findAllByCoachIdOrderByCreatedAtDesc(UUID coachId);

    List<TrainingPackage> findAllByPlayerIdAndCoachIdOrderByCreatedAtDesc(UUID playerId, UUID coachId);

    /** Najstarszy aktywny pakiet gracza u danego trenera (FIFO). */
    Optional<TrainingPackage> findFirstByPlayerIdAndCoachIdAndRemainingSessionsGreaterThanOrderByCreatedAtAsc(
            UUID playerId, UUID coachId, int minRemaining);
}
