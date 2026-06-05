package pl.pingpong.club.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.pingpong.club.model.Role;
import pl.pingpong.club.model.User;
import pl.pingpong.club.repository.UserRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByEmail("trener@pingpong.pl")) {
            userRepository.save(User.builder()
                    .firstName("Admin")
                    .lastName("Trener")
                    .email("trener@pingpong.pl")
                    .password(passwordEncoder.encode("Coach123!"))
                    .role(Role.ADMIN)
                    .build());
            log.info("Utworzono domyślne konto admina: trener@pingpong.pl");
        }
    }
}
