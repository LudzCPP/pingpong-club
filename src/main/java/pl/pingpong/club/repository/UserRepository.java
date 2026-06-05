package pl.pingpong.club.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.pingpong.club.model.Role;
import pl.pingpong.club.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByRole(Role role);

    /** Zawodnicy przypisani do danego trenera. */
    @Query("SELECT p FROM User coach JOIN coach.players p WHERE coach.id = :coachId")
    List<User> findPlayersByCoachId(@Param("coachId") UUID coachId);

    /** Czy trener ma danego zawodnika w swoim roster? */
    @Query("SELECT COUNT(p) > 0 FROM User coach JOIN coach.players p WHERE coach.id = :coachId AND p.id = :playerId")
    boolean existsCoachPlayerLink(@Param("coachId") UUID coachId, @Param("playerId") UUID playerId);

    /** Bezpośredni insert do tabeli coach_player. */
    @Modifying
    @Query(value = "INSERT INTO coach_player (coach_id, player_id) VALUES (:coachId, :playerId)", nativeQuery = true)
    void addCoachPlayerLink(@Param("coachId") UUID coachId, @Param("playerId") UUID playerId);
}
