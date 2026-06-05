package pl.pingpong.club.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.pingpong.club.model.JoinRequest;
import pl.pingpong.club.model.JoinRequestStatus;

import java.util.List;
import java.util.UUID;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, UUID> {

    List<JoinRequest> findAllByPlayerIdAndStatus(UUID playerId, JoinRequestStatus status);

    boolean existsByCoachIdAndPlayerIdAndStatus(UUID coachId, UUID playerId, JoinRequestStatus status);
}
