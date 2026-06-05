package pl.pingpong.club.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pingpong.club.config.JwtService;
import pl.pingpong.club.dto.AuthResponse;
import pl.pingpong.club.dto.InviteResponse;
import pl.pingpong.club.dto.LoginRequest;
import pl.pingpong.club.dto.RegisterRequest;
import pl.pingpong.club.exception.BusinessRuleException;
import pl.pingpong.club.model.InviteToken;
import pl.pingpong.club.model.Role;
import pl.pingpong.club.model.User;
import pl.pingpong.club.repository.InviteTokenRepository;
import pl.pingpong.club.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final InviteTokenRepository inviteTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessRuleException("Nie znaleziono użytkownika"));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getRole(),
                jwtService.extractExpiration(token));
    }

    /** Generuje link zaproszenia dla zawodnika (COACH lub ADMIN wywołuje). */
    @Transactional
    public InviteResponse generateInvite(String callerEmail) {
        User caller = userRepository.findByEmail(callerEmail)
                .orElseThrow(() -> new BusinessRuleException("Nie znaleziono użytkownika"));

        String rawToken = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(48);

        InviteToken invite = InviteToken.builder()
                .token(rawToken)
                .createdBy(caller)
                .expiresAt(expiresAt)
                .targetRole(Role.PLAYER)
                .build();

        inviteTokenRepository.save(invite);

        String inviteUrl = frontendUrl + "/register?token=" + rawToken;
        return new InviteResponse(inviteUrl, expiresAt);
    }

    /** Generuje link zaproszenia dla trenera (tylko ADMIN wywołuje). */
    @Transactional
    public InviteResponse generateCoachInvite(String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessRuleException("Nie znaleziono użytkownika"));

        String rawToken = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(48);

        InviteToken invite = InviteToken.builder()
                .token(rawToken)
                .createdBy(admin)
                .expiresAt(expiresAt)
                .targetRole(Role.COACH)
                .build();

        inviteTokenRepository.save(invite);

        String inviteUrl = frontendUrl + "/register?token=" + rawToken;
        return new InviteResponse(inviteUrl, expiresAt);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String token) {
        InviteToken invite = inviteTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessRuleException("Nieprawidłowy lub wygasły link zaproszenia"));

        if (invite.isUsed()) {
            throw new BusinessRuleException("Link zaproszenia został już wykorzystany");
        }
        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Link zaproszenia wygasł");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("Adres email jest już zajęty: " + request.email());
        }

        invite.setUsed(true);
        Role targetRole = invite.getTargetRole();

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(targetRole)
                .build();

        userRepository.save(user);

        if (targetRole == Role.PLAYER) {
            User creator = invite.getCreatedBy();
            if (creator.getRole() == Role.COACH) {
                userRepository.addCoachPlayerLink(creator.getId(), user.getId());
            }
        }

        String jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken, user.getEmail(), user.getRole(),
                jwtService.extractExpiration(jwtToken));
    }
}
