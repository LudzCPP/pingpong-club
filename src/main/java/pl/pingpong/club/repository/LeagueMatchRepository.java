package pl.pingpong.club.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.pingpong.club.model.LeagueMatch;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface LeagueMatchRepository extends JpaRepository<LeagueMatch, UUID> {

    List<LeagueMatch> findAllByPlayerIdOrderByMatchDateDesc(UUID playerId);

    @Query("""
            SELECT m FROM LeagueMatch m
            WHERE m.player.id = :playerId
              AND m.matchDate BETWEEN :from AND :to
            ORDER BY m.matchDate DESC
            """)
    List<LeagueMatch> findByPlayerAndPeriod(
            @Param("playerId") UUID playerId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("""
            SELECT m FROM LeagueMatch m
            WHERE m.matchDate BETWEEN :from AND :to
            ORDER BY m.matchDate DESC
            """)
    List<LeagueMatch> findAllByPeriod(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("""
            SELECT m FROM LeagueMatch m
            WHERE m.player.id IN :playerIds
              AND m.matchDate BETWEEN :from AND :to
            ORDER BY m.matchDate DESC
            """)
    List<LeagueMatch> findByPlayerIdsAndPeriod(
            @Param("playerIds") List<UUID> playerIds,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    List<LeagueMatch> findAllByPlayerIdInOrderByMatchDateDesc(List<UUID> playerIds);
}
