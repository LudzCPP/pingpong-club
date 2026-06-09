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
import pl.pingpong.club.model.PasswordResetToken;
import pl.pingpong.club.model.RefreshToken;
import pl.pingpong.club.model.Role;
import pl.pingpong.club.model.User;
import pl.pingpong.club.repository.InviteTokenRepository;
import pl.pingpong.club.repository.PasswordResetTokenRepository;
import pl.pingpong.club.repository.RefreshTokenRepository;
import pl.pingpong.club.repository.UserRepository;
import pl.pingpong.club.service.EmailService;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int REFRESH_TOKEN_DAYS = 30;

    private final UserRepository userRepository;
    private final InviteTokenRepository inviteTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessRuleException("Nie znaleziono użytkownika"));

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(rawRefreshToken)
                .orElseThrow(() -> new BusinessRuleException("Nieprawidłowy refresh token"));

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored);
            throw new BusinessRuleException("Refresh token wygasł — zaloguj się ponownie");
        }

        // Token rotation: delete old, issue new
        refreshTokenRepository.delete(stored);
        return buildAuthResponse(stored.getUser());
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

        User user;
        if (invite.getVirtualPlayer() != null) {
            // Przejęcie konta wirtualnego — aktualizujemy istniejącego usera
            user = invite.getVirtualPlayer();
            user.setFirstName(request.firstName());
            user.setLastName(request.lastName());
            user.setEmail(request.email());
            user.setPassword(passwordEncoder.encode(request.password()));
            user.setVirtual(false);
            userRepository.save(user);
        } else {
            // Nowe konto
            if (userRepository.existsByEmail(request.email())) {
                throw new BusinessRuleException("Adres email jest już zajęty: " + request.email());
            }
            Role targetRole = invite.getTargetRole();
            user = User.builder()
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
        }

        return buildAuthResponse(user);
    }

    /** Generuje link zaproszenia do przejęcia konta wirtualnego zawodnika. */
    @Transactional
    public InviteResponse generateVirtualPlayerInvite(String coachEmail, UUID virtualPlayerId, String playerEmail) {
        User coach = userRepository.findByEmail(coachEmail)
                .orElseThrow(() -> new BusinessRuleException("Nie znaleziono użytkownika"));
        User virtualPlayer = userRepository.findById(virtualPlayerId)
                .orElseThrow(() -> new BusinessRuleException("Nie znaleziono zawodnika"));

        if (!virtualPlayer.isVirtual()) {
            throw new BusinessRuleException("Ten zawodnik ma już prawdziwe konto");
        }
        if (!userRepository.existsCoachPlayerLink(coach.getId(), virtualPlayerId)) {
            throw new BusinessRuleException("Zawodnik nie należy do Twojej listy");
        }
        if (userRepository.existsByEmail(playerEmail)) {
            throw new BusinessRuleException("Podany adres email jest już zajęty");
        }

        String rawToken = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(48);

        InviteToken invite = InviteToken.builder()
                .token(rawToken)
                .createdBy(coach)
                .expiresAt(expiresAt)
                .targetRole(Role.PLAYER)
                .virtualPlayer(virtualPlayer)
                .build();

        inviteTokenRepository.save(invite);
        String inviteUrl = frontendUrl + "/register?token=" + rawToken;
        emailService.sendInviteEmail(playerEmail, inviteUrl, coach);
        return new InviteResponse(inviteUrl, expiresAt);
    }

    /**
     * Zawsze zwraca 200 — nie ujawnia czy email istnieje w bazie.
     * Jeśli email jest znany, wysyła link reset hasła ważny 1h.
     */
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String rawToken = UUID.randomUUID().toString().replace("-", "");
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(rawToken)
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();
            passwordResetTokenRepository.save(resetToken);

            String resetUrl = frontendUrl + "/reset-password?token=" + rawToken;
            emailService.sendPasswordResetEmail(user, resetUrl);
        });
    }

    private AuthResponse buildAuthResponse(User user) {
        String jwt = jwtService.generateToken(user);
        String raw = UUID.randomUUID().toString().replace("-", "");
        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .token(raw)
                .expiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_DAYS))
                .build();
        refreshTokenRepository.save(rt);
        return new AuthResponse(jwt, raw, user.getEmail(), user.getRole(),
                jwtService.extractExpiration(jwt), user.getFirstName());
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessRuleException("Nieprawidłowy lub wygasły link resetowania hasła"));

        if (resetToken.isUsed()) {
            throw new BusinessRuleException("Link resetowania hasła został już wykorzystany");
        }
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Link resetowania hasła wygasł");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }
}
