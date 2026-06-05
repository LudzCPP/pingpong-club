package pl.pingpong.club.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pingpong.club.dto.ChangePasswordRequest;
import pl.pingpong.club.dto.CreateCoachRequest;
import pl.pingpong.club.dto.UserResponse;
import pl.pingpong.club.exception.BusinessRuleException;
import pl.pingpong.club.exception.ResourceNotFoundException;
import pl.pingpong.club.mapper.UserMapper;
import pl.pingpong.club.model.Role;
import pl.pingpong.club.model.User;
import pl.pingpong.club.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getMyProfile(String email) {
        return userMapper.toResponse(findByEmail(email));
    }

    public List<UserResponse> getAllPlayers() {
        return userRepository.findAllByRole(Role.PLAYER).stream()
                .map(userMapper::toResponse)
                .toList();
    }

    public List<UserResponse> getAllCoaches() {
        return userRepository.findAllByRole(Role.COACH).stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse createCoach(CreateCoachRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("Adres email jest już zajęty: " + request.email());
        }

        User coach = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.COACH)
                .build();

        return userMapper.toResponse(userRepository.save(coach));
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = findByEmail(email);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessRuleException("Podane aktualne hasło jest nieprawidłowe");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public UserResponse deactivateUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika o ID: " + id));
        user.setActive(false);
        return userMapper.toResponse(userRepository.save(user));
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika: " + email));
    }
}
