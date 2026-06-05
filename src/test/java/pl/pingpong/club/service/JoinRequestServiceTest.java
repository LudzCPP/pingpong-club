package pl.pingpong.club.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.pingpong.club.dto.JoinRequestResponse;
import pl.pingpong.club.exception.BusinessRuleException;
import pl.pingpong.club.exception.ResourceNotFoundException;
import pl.pingpong.club.model.JoinRequest;
import pl.pingpong.club.model.JoinRequestStatus;
import pl.pingpong.club.model.Role;
import pl.pingpong.club.model.User;
import pl.pingpong.club.repository.JoinRequestRepository;
import pl.pingpong.club.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JoinRequestServiceTest {

    @Mock private JoinRequestRepository joinRequestRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;

    @InjectMocks private JoinRequestService service;

    // ---- sendRequest ----

    @Test
    void sendRequest_valid_savesAndReturnsResponse() {
        User coach = coachUser();
        User player = playerUser();

        given(userRepository.findByEmail("coach@test.pl")).willReturn(Optional.of(coach));
        given(userRepository.findByEmail("player@test.pl")).willReturn(Optional.of(player));
        given(userRepository.existsCoachPlayerLink(coach.getId(), player.getId())).willReturn(false);
        given(joinRequestRepository.existsByCoachIdAndPlayerIdAndStatus(
                coach.getId(), player.getId(), JoinRequestStatus.PENDING)).willReturn(false);
        given(joinRequestRepository.save(any())).willAnswer(inv -> {
            JoinRequest r = inv.getArgument(0);
            r = JoinRequest.builder()
                    .id(UUID.randomUUID()).coach(r.getCoach()).player(r.getPlayer())
                    .status(r.getStatus()).createdAt(LocalDateTime.now()).build();
            return r;
        });

        JoinRequestResponse response = service.sendRequest("coach@test.pl", "player@test.pl");

        assertThat(response.status()).isEqualTo(JoinRequestStatus.PENDING);
        assertThat(response.coachEmail()).isEqualTo("coach@test.pl");
        verify(joinRequestRepository).save(any());
    }

    @Test
    void sendRequest_playerAlreadyLinked_throwsBusinessRuleException() {
        User coach = coachUser();
        User player = playerUser();

        given(userRepository.findByEmail("coach@test.pl")).willReturn(Optional.of(coach));
        given(userRepository.findByEmail("player@test.pl")).willReturn(Optional.of(player));
        given(userRepository.existsCoachPlayerLink(coach.getId(), player.getId())).willReturn(true);

        assertThatThrownBy(() -> service.sendRequest("coach@test.pl", "player@test.pl"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("już przypisany");
    }

    @Test
    void sendRequest_pendingRequestExists_throwsBusinessRuleException() {
        User coach = coachUser();
        User player = playerUser();

        given(userRepository.findByEmail("coach@test.pl")).willReturn(Optional.of(coach));
        given(userRepository.findByEmail("player@test.pl")).willReturn(Optional.of(player));
        given(userRepository.existsCoachPlayerLink(coach.getId(), player.getId())).willReturn(false);
        given(joinRequestRepository.existsByCoachIdAndPlayerIdAndStatus(
                coach.getId(), player.getId(), JoinRequestStatus.PENDING)).willReturn(true);

        assertThatThrownBy(() -> service.sendRequest("coach@test.pl", "player@test.pl"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("oczekujące");
    }

    @Test
    void sendRequest_targetIsCoach_throwsBusinessRuleException() {
        User coach = coachUser();
        User anotherCoach = User.builder().id(UUID.randomUUID()).firstName("X").lastName("Y")
                .email("other@test.pl").password("x").role(Role.COACH).build();

        given(userRepository.findByEmail("coach@test.pl")).willReturn(Optional.of(coach));
        given(userRepository.findByEmail("other@test.pl")).willReturn(Optional.of(anotherCoach));

        assertThatThrownBy(() -> service.sendRequest("coach@test.pl", "other@test.pl"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("nie jest zawodnikiem");
    }

    @Test
    void sendRequest_playerNotFound_throwsResourceNotFoundException() {
        User coach = coachUser();
        given(userRepository.findByEmail("coach@test.pl")).willReturn(Optional.of(coach));
        given(userRepository.findByEmail("ghost@test.pl")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendRequest("coach@test.pl", "ghost@test.pl"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- getPendingRequests ----

    @Test
    void getPendingRequests_returnsOnlyPendingForPlayer() {
        User coach = coachUser();
        User player = playerUser();
        JoinRequest pending = pendingRequest(coach, player);

        given(userRepository.findByEmail("player@test.pl")).willReturn(Optional.of(player));
        given(joinRequestRepository.findAllByPlayerIdAndStatus(player.getId(), JoinRequestStatus.PENDING))
                .willReturn(List.of(pending));

        List<JoinRequestResponse> result = service.getPendingRequests("player@test.pl");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(JoinRequestStatus.PENDING);
    }

    // ---- acceptRequest ----

    @Test
    void acceptRequest_pendingRequest_linksCoachPlayerAndReturnsAccepted() {
        User coach = coachUser();
        User player = playerUser();
        UUID requestId = UUID.randomUUID();
        JoinRequest request = pendingRequest(coach, player);

        given(userRepository.findByEmail("player@test.pl")).willReturn(Optional.of(player));
        given(joinRequestRepository.findById(requestId)).willReturn(Optional.of(request));
        given(joinRequestRepository.save(request)).willReturn(request);

        JoinRequestResponse response = service.acceptRequest(requestId, "player@test.pl");

        assertThat(response.status()).isEqualTo(JoinRequestStatus.ACCEPTED);
        verify(userRepository).addCoachPlayerLink(coach.getId(), player.getId());
    }

    @Test
    void acceptRequest_alreadyAccepted_throwsBusinessRuleException() {
        User coach = coachUser();
        User player = playerUser();
        UUID requestId = UUID.randomUUID();
        JoinRequest request = pendingRequest(coach, player);
        request.setStatus(JoinRequestStatus.ACCEPTED);

        given(userRepository.findByEmail("player@test.pl")).willReturn(Optional.of(player));
        given(joinRequestRepository.findById(requestId)).willReturn(Optional.of(request));

        assertThatThrownBy(() -> service.acceptRequest(requestId, "player@test.pl"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("rozpatrzone");
    }

    @Test
    void acceptRequest_wrongPlayer_throws404() {
        User coach = coachUser();
        User player = playerUser();
        User otherPlayer = User.builder().id(UUID.randomUUID()).firstName("X").lastName("Y")
                .email("other@test.pl").password("x").role(Role.PLAYER).build();
        UUID requestId = UUID.randomUUID();
        JoinRequest request = pendingRequest(coach, player);

        given(userRepository.findByEmail("other@test.pl")).willReturn(Optional.of(otherPlayer));
        given(joinRequestRepository.findById(requestId)).willReturn(Optional.of(request));

        assertThatThrownBy(() -> service.acceptRequest(requestId, "other@test.pl"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- rejectRequest ----

    @Test
    void rejectRequest_pendingRequest_setsStatusRejected() {
        User coach = coachUser();
        User player = playerUser();
        UUID requestId = UUID.randomUUID();
        JoinRequest request = pendingRequest(coach, player);

        given(userRepository.findByEmail("player@test.pl")).willReturn(Optional.of(player));
        given(joinRequestRepository.findById(requestId)).willReturn(Optional.of(request));
        given(joinRequestRepository.save(request)).willReturn(request);

        JoinRequestResponse response = service.rejectRequest(requestId, "player@test.pl");

        assertThat(response.status()).isEqualTo(JoinRequestStatus.REJECTED);
    }

    // ---- helpers ----

    private User coachUser() {
        return User.builder().id(UUID.randomUUID()).firstName("Piotr").lastName("Trener")
                .email("coach@test.pl").password("x").role(Role.COACH).build();
    }

    private User playerUser() {
        return User.builder().id(UUID.randomUUID()).firstName("Anna").lastName("Nowak")
                .email("player@test.pl").password("x").role(Role.PLAYER).build();
    }

    private JoinRequest pendingRequest(User coach, User player) {
        return JoinRequest.builder()
                .id(UUID.randomUUID())
                .coach(coach)
                .player(player)
                .status(JoinRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
