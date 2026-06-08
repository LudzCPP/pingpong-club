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
import pl.pingpong.club.dto.ChangePasswordRequest;
import pl.pingpong.club.dto.CreateCoachRequest;
import pl.pingpong.club.dto.CreateVirtualPlayerRequest;
import pl.pingpong.club.dto.InviteVirtualPlayerRequest;
import pl.pingpong.club.dto.UpdateProfileRequest;
import pl.pingpong.club.dto.UserResponse;
import pl.pingpong.club.exception.BusinessRuleException;
import pl.pingpong.club.model.Role;
import pl.pingpong.club.service.AuthService;
import pl.pingpong.club.service.UserService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = UserController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
        }
)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserService userService;
    @MockBean AuthService authService;

    @Test
    @WithMockUser(username = "player@test.pl", roles = "PLAYER")
    void getMyProfile_authenticated_returns200() throws Exception {
        given(userService.getMyProfile(anyString())).willReturn(playerResponse());

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("player@test.pl"));
    }

    @Test
    void getMyProfile_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void getPlayers_asCoach_returns200() throws Exception {
        given(userService.getAllPlayers(anyString())).willReturn(List.of(playerResponse()));

        mockMvc.perform(get("/users/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("PLAYER"));
    }

    @Test
    @WithMockUser(username = "player@test.pl", roles = "PLAYER")
    void getPlayers_asPlayer_returns403() throws Exception {
        mockMvc.perform(get("/users/players"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@test.pl", roles = "ADMIN")
    void createCoach_asAdmin_returns201() throws Exception {
        given(userService.createCoach(any())).willReturn(coachResponse());
        CreateCoachRequest request = new CreateCoachRequest("Jan", "Trener", "jan@test.pl", "Password1");

        mockMvc.perform(post("/users/coaches")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("COACH"));
    }

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void createCoach_asCoach_returns403() throws Exception {
        CreateCoachRequest request = new CreateCoachRequest("Jan", "T", "jan@test.pl", "Password1");

        mockMvc.perform(post("/users/coaches")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "player@test.pl", roles = "PLAYER")
    void createCoach_asPlayer_returns403() throws Exception {
        CreateCoachRequest request = new CreateCoachRequest("Jan", "T", "jan@test.pl", "Password1");

        mockMvc.perform(post("/users/coaches")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@test.pl", roles = "ADMIN")
    void deactivateUser_asAdmin_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        given(userService.deactivateUser(id)).willReturn(playerResponse());

        mockMvc.perform(delete("/users/{id}", id).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void deactivateUser_asCoach_returns403() throws Exception {
        mockMvc.perform(delete("/users/{id}", UUID.randomUUID()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "player@test.pl", roles = "PLAYER")
    void changePassword_validRequest_returns204() throws Exception {
        doNothing().when(userService).changePassword(anyString(), any());

        mockMvc.perform(patch("/users/me/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("oldPass1", "newPass1"))))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "player@test.pl", roles = "PLAYER")
    void changePassword_wrongCurrent_returns422() throws Exception {
        doThrow(new BusinessRuleException("nieprawidłowe"))
                .when(userService).changePassword(anyString(), any());

        mockMvc.perform(patch("/users/me/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("wrongPass", "newPass1"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(username = "player@test.pl", roles = "PLAYER")
    void changePassword_tooShortNew_returns400() throws Exception {
        mockMvc.perform(patch("/users/me/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("oldPass1", "abc"))))
                .andExpect(status().isBadRequest());
    }

    // ---- PATCH /users/me (updateProfile) ----

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void updateProfile_validRequest_returns200() throws Exception {
        given(userService.updateProfile(anyString(), any())).willReturn(coachResponse());

        mockMvc.perform(patch("/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileRequest("Jan", "Trener"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jan"));
    }

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void updateProfile_blankFirstName_returns400() throws Exception {
        mockMvc.perform(patch("/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileRequest("", "Trener"))))
                .andExpect(status().isBadRequest());
    }

    // ---- PATCH /users/{id}/activate ----

    @Test
    @WithMockUser(username = "admin@test.pl", roles = "ADMIN")
    void activateUser_asAdmin_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        given(userService.activateUser(id)).willReturn(playerResponse());

        mockMvc.perform(patch("/users/{id}/activate", id).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void activateUser_asCoach_returns403() throws Exception {
        mockMvc.perform(patch("/users/{id}/activate", UUID.randomUUID()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "player@test.pl", roles = "PLAYER")
    void activateUser_asPlayer_returns403() throws Exception {
        mockMvc.perform(patch("/users/{id}/activate", UUID.randomUUID()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ---- POST /users/players/virtual ----

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void createVirtualPlayer_asCoach_returns201() throws Exception {
        given(userService.createVirtualPlayer(anyString(), any())).willReturn(virtualPlayerResponse());

        mockMvc.perform(post("/users/players/virtual")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateVirtualPlayerRequest("Jan", "Wirtualny"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.virtual").value(true));
    }

    @Test
    @WithMockUser(username = "player@test.pl", roles = "PLAYER")
    void createVirtualPlayer_asPlayer_returns403() throws Exception {
        mockMvc.perform(post("/users/players/virtual")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateVirtualPlayerRequest("Jan", "Wirtualny"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void createVirtualPlayer_blankName_returns400() throws Exception {
        mockMvc.perform(post("/users/players/virtual")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateVirtualPlayerRequest("", "Wirtualny"))))
                .andExpect(status().isBadRequest());
    }

    // ---- POST /users/players/{id}/invite ----

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void inviteVirtualPlayer_asCoach_returns204() throws Exception {
        given(authService.generateVirtualPlayerInvite(anyString(), any(), anyString())).willReturn(null);

        mockMvc.perform(post("/users/players/{id}/invite", UUID.randomUUID())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InviteVirtualPlayerRequest("player@example.pl"))))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "player@test.pl", roles = "PLAYER")
    void inviteVirtualPlayer_asPlayer_returns403() throws Exception {
        mockMvc.perform(post("/users/players/{id}/invite", UUID.randomUUID())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InviteVirtualPlayerRequest("player@example.pl"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void inviteVirtualPlayer_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/users/players/{id}/invite", UUID.randomUUID())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InviteVirtualPlayerRequest("not-an-email"))))
                .andExpect(status().isBadRequest());
    }

    // ---- DELETE /users/players/{playerId} ----

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void removePlayer_asCoach_returns204() throws Exception {
        doNothing().when(userService).removePlayerFromCoach(anyString(), any());

        mockMvc.perform(delete("/users/players/{id}", UUID.randomUUID()).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "player@test.pl", roles = "PLAYER")
    void removePlayer_asPlayer_returns403() throws Exception {
        mockMvc.perform(delete("/users/players/{id}", UUID.randomUUID()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    private UserResponse playerResponse() {
        return new UserResponse(UUID.randomUUID(), "Anna", "Nowak", "player@test.pl", Role.PLAYER, true, false);
    }

    private UserResponse coachResponse() {
        return new UserResponse(UUID.randomUUID(), "Jan", "Trener", "jan@test.pl", Role.COACH, true, false);
    }

    private UserResponse virtualPlayerResponse() {
        return new UserResponse(UUID.randomUUID(), "Jan", "Wirtualny", "virtual-x@ttmanager.internal", Role.PLAYER, true, true);
    }
}
