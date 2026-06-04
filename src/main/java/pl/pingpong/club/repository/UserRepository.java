package pl.pingpong.club.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pingpong.club.model.Role;
import pl.pingpong.club.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByRole(Role role);
}
