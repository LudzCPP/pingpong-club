package pl.pingpong.club.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pingpong.club.model.InviteToken;

import java.util.Optional;
import java.util.UUID;

public interface InviteTokenRepository extends JpaRepository<InviteToken, UUID> {

    Optional<InviteToken> findByToken(String token);
}
