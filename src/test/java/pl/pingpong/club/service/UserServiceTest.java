package pl.pingpong.club.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserService userService;

    @Test
    void getMyProfile_returnsUserResponse() {
        User user = playerUser();
        UserResponse response = mock(UserResponse.class);
        given(userRepository.findByEmail("player@test.pl")).willReturn(Optional.of(user));
        given(userMapper.toResponse(user)).willReturn(response);

        assertThat(userService.getMyProfile("player@test.pl")).isEqualTo(response);
    }

    @Test
    void getAllPlayers_asAdmin_queriesAllPlayers() {
        User admin = adminUser();
        User p = playerUser();
        given(userRepository.findByEmail("admin@test.pl")).willReturn(Optional.of(admin));
        given(userRepository.findAllByRole(Role.PLAYER)).willReturn(List.of(p));
        given(userMapper.toResponse(p)).willReturn(mock(UserResponse.class));

        List<UserResponse> result = userService.getAllPlayers("admin@test.pl");

        assertThat(result).hasSize(1);
        verify(userRepository).findAllByRole(Role.PLAYER);
    }

    @Test
    void getAllPlayers_asCoach_queriesOwnPlayers() {
        User coach = coachUser();
        User p = playerUser();
        given(userRepository.findByEmail("coach@test.pl")).willReturn(Optional.of(coach));
        given(userRepository.findPlayersByCoachId(coach.getId())).willReturn(List.of(p));
        given(userMapper.toResponse(p)).willReturn(mock(UserResponse.class));

        List<UserResponse> result = userService.getAllPlayers("coach@test.pl");

        assertThat(result).hasSize(1);
        verify(userRepository).findPlayersByCoachId(coach.getId());
    }

    @Test
    void getAllCoaches_queriesOnlyCoachRole() {
        User c = coachUser();
        given(userRepository.findAllByRole(Role.COACH)).willReturn(List.of(c));
        given(userMapper.toResponse(c)).willReturn(mock(UserResponse.class));

        List<UserResponse> result = userService.getAllCoaches();

        assertThat(result).hasSize(1);
        verify(userRepository).findAllByRole(Role.COACH);
    }

    @Test
    void createCoach_savesUserWithCoachRole() {
        CreateCoachRequest request = new CreateCoachRequest("Jan", "Trener", "jan@test.pl", "Password1");
        given(userRepository.existsByEmail("jan@test.pl")).willReturn(false);
        given(passwordEncoder.encode("Password1")).willReturn("hashed");
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(userMapper.toResponse(any())).willReturn(mock(UserResponse.class));

        userService.createCoach(request);

        verify(userRepository).save(argThat(u -> u.getRole() == Role.COACH
                && "jan@test.pl".equals(u.getEmail())));
    }

    @Test
    void createCoach_duplicateEmail_throwsBusinessRuleException() {
        given(userRepository.existsByEmail("jan@test.pl")).willReturn(true);

        assertThatThrownBy(() -> userService.createCoach(
                new CreateCoachRequest("Jan", "T", "jan@test.pl", "Password1")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("jan@test.pl");
    }

    @Test
    void deactivateUser_setsActiveFalse() {
        UUID id = UUID.randomUUID();
        User user = playerUser();
        given(userRepository.findById(id)).willReturn(Optional.of(user));
        given(userRepository.save(user)).willReturn(user);
        given(userMapper.toResponse(user)).willReturn(mock(UserResponse.class));

        userService.deactivateUser(id);

        assertThat(user.isActive()).isFalse();
    }

    @Test
    void deactivateUser_notFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        given(userRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deactivateUser(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changePassword_correctCurrent_updatesPassword() {
        User user = playerUser();
        user.setPassword("hashed_old");
        given(userRepository.findByEmail("player@test.pl")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("oldPass", "hashed_old")).willReturn(true);
        given(passwordEncoder.encode("newPass1")).willReturn("hashed_new");

        userService.changePassword("player@test.pl", new ChangePasswordRequest("oldPass", "newPass1"));

        verify(userRepository).save(argThat(u -> "hashed_new".equals(u.getPassword())));
    }

    @Test
    void changePassword_wrongCurrent_throwsBusinessRuleException() {
        User user = playerUser();
        user.setPassword("hashed_old");
        given(userRepository.findByEmail("player@test.pl")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongPass", "hashed_old")).willReturn(false);

        assertThatThrownBy(() -> userService.changePassword(
                "player@test.pl", new ChangePasswordRequest("wrongPass", "newPass1")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("nieprawidłowe");
    }

    private User playerUser() {
        return User.builder().id(UUID.randomUUID()).firstName("Anna").lastName("Nowak")
                .email("player@test.pl").password("x").role(Role.PLAYER).build();
    }

    private User coachUser() {
        return User.builder().id(UUID.randomUUID()).firstName("Piotr").lastName("Trener")
                .email("coach@test.pl").password("x").role(Role.COACH).build();
    }

    private User adminUser() {
        return User.builder().id(UUID.randomUUID()).firstName("Admin").lastName("Root")
                .email("admin@test.pl").password("x").role(Role.ADMIN).build();
    }
}
