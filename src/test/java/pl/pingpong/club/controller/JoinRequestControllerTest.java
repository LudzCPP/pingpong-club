package pl.pingpong.club.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pl.pingpong.club.config.JwtAuthFilter;
import pl.pingpong.club.config.SecurityConfig;
import pl.pingpong.club.config.TestSecurityConfig;
import pl.pingpong.club.dto.JoinRequestResponse;
import pl.pingpong.club.dto.SendJoinRequestRequest;
import pl.pingpong.club.model.JoinRequestStatus;
import pl.pingpong.club.service.JoinRequestService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = JoinRequestController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
        }
)
@Import(TestSecurityConfig.class)
class JoinRequestControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean JoinRequestService joinRequestService;

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void sendRequest_asCoach_returns201() throws Exception {
        given(joinRequestService.sendRequest(anyString(), anyString())).willReturn(pendingResponse());

        mockMvc.perform(post("/join-requests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendJoinRequestRequest("player@test.pl"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "player@test.pl", roles = "PLAYER")
    void sendRequest_asPlayer_returns403() throws Exception {
        mockMvc.perform(post("/join-requests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendJoinRequestRequest("other@test.pl"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "player@test.pl", roles = "PLAYER")
    void getPending_asPlayer_returns200() throws Exception {
        given(joinRequestService.getPendingRequests(anyString())).willReturn(List.of(pendingResponse()));

        mockMvc.perform(get("/join-requests/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void getPending_asCoach_returns403() throws Exception {
        mockMvc.perform(get("/join-requests/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "player@test.pl", roles = "PLAYER")
    void accept_asPlayer_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        given(joinRequestService.acceptRequest(any(), anyString()))
                .willReturn(response(JoinRequestStatus.ACCEPTED));

        mockMvc.perform(patch("/join-requests/{id}/accept", id).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    @WithMockUser(username = "player@test.pl", roles = "PLAYER")
    void reject_asPlayer_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        given(joinRequestService.rejectRequest(any(), anyString()))
                .willReturn(response(JoinRequestStatus.REJECTED));

        mockMvc.perform(patch("/join-requests/{id}/reject", id).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void accept_asCoach_returns403() throws Exception {
        mockMvc.perform(patch("/join-requests/{id}/accept", UUID.randomUUID()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    private JoinRequestResponse pendingResponse() {
        return response(JoinRequestStatus.PENDING);
    }

    private JoinRequestResponse response(JoinRequestStatus status) {
        return new JoinRequestResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Piotr",
                "Trener",
                "coach@test.pl",
                status,
                LocalDateTime.now()
        );
    }
}
