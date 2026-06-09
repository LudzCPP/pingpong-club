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
import pl.pingpong.club.dto.AuthResponse;
import pl.pingpong.club.dto.InviteResponse;
import pl.pingpong.club.dto.LoginRequest;
import pl.pingpong.club.dto.RegisterRequest;
import pl.pingpong.club.model.Role;
import pl.pingpong.club.service.AuthService;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = AuthController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
        }
)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService authService;

    @Test
    void login_validRequest_returns200WithToken() throws Exception {
        given(authService.login(any())).willReturn(authResponse());

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("test@test.pl", "password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("PLAYER"));
    }

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void generateInvite_asCoach_returns201WithUrl() throws Exception {
        given(authService.generateInvite("coach@test.pl"))
                .willReturn(new InviteResponse("http://localhost:5173/register?token=abc", LocalDateTime.now().plusHours(48)));

        mockMvc.perform(post("/auth/invite").with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inviteUrl").value("http://localhost:5173/register?token=abc"));
    }

    @Test
    @WithMockUser(roles = "PLAYER")
    void generateInvite_asPlayer_returns403() throws Exception {
        mockMvc.perform(post("/auth/invite").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void register_validTokenAndRequest_returns201() throws Exception {
        given(authService.register(any(), eq("valid-token"))).willReturn(authResponse());

        mockMvc.perform(post("/auth/register")
                        .param("token", "valid-token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Jan", "Kowalski", "new@test.pl", "Password1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PLAYER"));
    }

    @Test
    void register_missingToken_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Jan", "K", "new@test.pl", "Password1"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .param("token", "valid-token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Jan", "K", "not-an-email", "Password1"))))
                .andExpect(status().isBadRequest());
    }

    private AuthResponse authResponse() {
        return new AuthResponse("jwt-token", "refresh-token", "test@test.pl", Role.PLAYER, Instant.now().plusSeconds(3600), "Jan");
    }
}
