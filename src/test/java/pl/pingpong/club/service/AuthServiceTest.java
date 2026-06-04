package pl.pingpong.club.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.pingpong.club.config.JwtService;
import pl.pingpong.club.dto.LoginRequest;
import pl.pingpong.club.dto.RegisterRequest;
import pl.pingpong.club.exception.BusinessRuleException;
import pl.pingpong.club.model.Role;
import pl.pingpong.club.model.User;
import pl.pingpong.club.repository.UserRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthService authService;

    @Test
    void login_validCredentials_returnsTokenAndRole() {
        User user = playerUser();
        given(userRepository.findByEmail("player@test.pl")).willReturn(Optional.of(user));
        given(jwtService.generateToken(user)).willReturn("jwt");
        given(jwtService.extractExpiration("jwt")).willReturn(Instant.now().plusSeconds(3600));

        var response = authService.login(new LoginRequest("player@test.pl", "pass"));

        assertThat(response.token()).isEqualTo("jwt");
        assertThat(response.role()).isEqualTo(Role.PLAYER);
        assertThat(response.email()).isEqualTo("player@test.pl");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void register_newUser_alwaysCreatesPlayerRole() {
        given(userRepository.existsByEmail("jan@test.pl")).willReturn(false);
        given(passwordEncoder.encode("Password1")).willReturn("hashed");
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(jwtService.generateToken(any())).willReturn("jwt");
        given(jwtService.extractExpiration(any())).willReturn(Instant.now().plusSeconds(3600));

        var response = authService.register(
                new RegisterRequest("Jan", "Kowalski", "jan@test.pl", "Password1"));

        assertThat(response.role()).isEqualTo(Role.PLAYER);
        assertThat(response.email()).isEqualTo("jan@test.pl");
    }

    @Test
    void register_duplicateEmail_throwsBusinessRuleException() {
        given(userRepository.existsByEmail("jan@test.pl")).willReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Jan", "K", "jan@test.pl", "Password1")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("jan@test.pl");
    }

    private User playerUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .firstName("Anna")
                .lastName("Nowak")
                .email("player@test.pl")
                .password("hashed")
                .role(Role.PLAYER)
                .build();
    }
}
