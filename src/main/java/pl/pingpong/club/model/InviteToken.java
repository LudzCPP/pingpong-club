package pl.pingpong.club.model;

import jakarta.persistence.*;
import lombok.*;
import pl.pingpong.club.model.Role;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invite_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role targetRole = Role.PLAYER;

    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;
}
