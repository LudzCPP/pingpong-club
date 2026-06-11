package pl.pingpong.club.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import pl.pingpong.club.config.JwtService;
import pl.pingpong.club.dto.LoginRequest;
import pl.pingpong.club.dto.RegisterRequest;
import pl.pingpong.club.exception.BusinessRuleException;
import pl.pingpong.club.model.InviteToken;
import pl.pingpong.club.model.Role;
import pl.pingpong.club.model.User;
import pl.pingpong.club.repository.InviteTokenRepository;
import pl.pingpong.club.repository.RefreshTokenRepository;
import pl.pingpong.club.repository.UserRepository;

import java.time.Instant;
import java.time.LocalDateTime;
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
    @Mock private InviteTokenRepository inviteTokenRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "frontendUrl", "http://localhost:5173");
    }

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
    void generateInvite_coach_returnsUrlWithToken() {
        User coach = coachUser();
        given(userRepository.findByEmail("coach@test.pl")).willReturn(Optional.of(coach));
        given(inviteTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        var response = authService.generateInvite("coach@test.pl");

        assertThat(response.inviteUrl()).startsWith("http://localhost:5173/register?token=");
        assertThat(response.expiresAt()).isAfter(LocalDateTime.now().plusHours(47));
    }

    @Test
    void register_validToken_createsPlayerAndReturnsJwt() {
        InviteToken invite = validInvite();
        given(inviteTokenRepository.findByToken("abc123")).willReturn(Optional.of(invite));
        given(userRepository.existsByEmail("jan@test.pl")).willReturn(false);
        given(passwordEncoder.encode("Password1")).willReturn("hashed");
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(jwtService.generateToken(any())).willReturn("jwt");
        given(jwtService.extractExpiration(any())).willReturn(Instant.now().plusSeconds(3600));

        var response = authService.register(
                new RegisterRequest("Jan", "Kowalski", "jan@test.pl", "Password1"), "abc123");

        assertThat(response.role()).isEqualTo(Role.PLAYER);
        assertThat(invite.isUsed()).isTrue();
    }

    @Test
    void register_invalidToken_throwsBusinessRuleException() {
        given(inviteTokenRepository.findByToken("bad")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Jan", "K", "jan@test.pl", "Password1"), "bad"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Nieprawidłowy");
    }

    @Test
    void register_usedToken_throwsBusinessRuleException() {
        InviteToken used = validInvite();
        used.setUsed(true);
        given(inviteTokenRepository.findByToken("abc123")).willReturn(Optional.of(used));

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Jan", "K", "jan@test.pl", "Password1"), "abc123"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("już wykorzystany");
    }

    @Test
    void register_expiredToken_throwsBusinessRuleException() {
        InviteToken expired = InviteToken.builder()
                .token("abc123")
                .createdBy(coachUser())
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();
        given(inviteTokenRepository.findByToken("abc123")).willReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Jan", "K", "jan@test.pl", "Password1"), "abc123"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("wygasł");
    }

    @Test
    void register_duplicateEmail_throwsBusinessRuleException() {
        given(inviteTokenRepository.findByToken("abc123")).willReturn(Optional.of(validInvite()));
        given(userRepository.existsByEmail("jan@test.pl")).willReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Jan", "K", "jan@test.pl", "Password1"), "abc123"))
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

    private User coachUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .firstName("Trener")
                .lastName("Kowalski")
                .email("coach@test.pl")
                .password("hashed")
                .role(Role.COACH)
                .build();
    }

    private InviteToken validInvite() {
        return InviteToken.builder()
                .token("abc123")
                .createdBy(coachUser())
                .expiresAt(LocalDateTime.now().plusHours(48))
                .build();
    }
}
