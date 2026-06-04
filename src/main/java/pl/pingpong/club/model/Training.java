package pl.pingpong.club.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "trainings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Training {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * Zawsze w formacie "trening [Imię]", np. "trening Janusz".
     * Wartość ustawiana przez TrainingService – nigdy bezpośrednio.
     */
    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private User player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coach_id", nullable = false)
    private User coach;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    /** Czas trwania w minutach (np. 60, 90). */
    @Column(nullable = false)
    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TrainingStatus status = TrainingStatus.SCHEDULED;

    /** Stawka godzinowa obowiązująca w momencie rezerwacji. */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal hourlyRate;

    @Column(length = 500)
    private String notes;
}
