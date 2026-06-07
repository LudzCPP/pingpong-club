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
import pl.pingpong.club.dto.UserResponse;
import pl.pingpong.club.exception.BusinessRuleException;
import pl.pingpong.club.model.Role;
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

    private UserResponse playerResponse() {
        return new UserResponse(UUID.randomUUID(), "Anna", "Nowak", "player@test.pl", Role.PLAYER, true, false);
    }

    private UserResponse coachResponse() {
        return new UserResponse(UUID.randomUUID(), "Jan", "Trener", "jan@test.pl", Role.COACH, true, false);
    }
}
