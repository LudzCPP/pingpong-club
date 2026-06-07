package pl.pingpong.club.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pingpong.club.model.PasswordResetToken;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByToken(String token);
}
