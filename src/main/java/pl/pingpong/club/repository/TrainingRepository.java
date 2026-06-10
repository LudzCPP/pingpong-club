package pl.pingpong.club.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.pingpong.club.model.Training;
import pl.pingpong.club.model.TrainingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TrainingRepository extends JpaRepository<Training, UUID> {

    List<Training> findAllByPlayerId(UUID playerId);

    List<Training> findAllByCoachId(UUID coachId);

    List<Training> findAllByStatus(TrainingStatus status);

    @Query("""
            SELECT t FROM Training t
            WHERE t.player.id = :playerId
              AND t.scheduledAt BETWEEN :from AND :to
              AND t.status = :status
            """)
    List<Training> findByPlayerAndPeriodAndStatus(
            @Param("playerId") UUID playerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") TrainingStatus status
    );

    /**
     * Natywne SQL (PostgreSQL) do wykrywania nakładania się terminów trenera.
     * Warunek nakładania: A.start < B.end AND A.end > B.start
     */
    @Query(value = """
            SELECT COUNT(*) > 0
            FROM trainings
            WHERE coach_id = :coachId
              AND status <> 'CANCELLED'
              AND scheduled_at < :end
              AND scheduled_at + (duration_minutes * INTERVAL '1 minute') > :start
            """, nativeQuery = true)
    boolean existsConflictForCoach(
            @Param("coachId") UUID coachId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<Training> findAllByRecurringGroupIdOrderByScheduledAt(UUID recurringGroupId);

    long countByStatus(TrainingStatus status);

    @Query("SELECT COALESCE(SUM(t.totalPrice), 0) FROM Training t WHERE t.status = :status")
    java.math.BigDecimal sumTotalPriceByStatus(@Param("status") TrainingStatus status);
}
