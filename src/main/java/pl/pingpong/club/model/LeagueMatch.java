package pl.pingpong.club.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "league_matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeagueMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private User player;

    @Column(nullable = false)
    private LocalDate matchDate;

    @Column(nullable = false, length = 200)
    private String opponent;

    /** Wynik meczu, np. "3:1", "0:3". */
    @Column(nullable = false, length = 10)
    private String result;

    /** Kwota wypłaty dla zawodnika za udział w meczu (może być 0). */
    @Column(nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal payment = BigDecimal.ZERO;

    @Column(length = 500)
    private String notes;
}
