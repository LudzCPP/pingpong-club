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
import org.springframework.test.web.servlet.MockMvc;
import pl.pingpong.club.config.JwtAuthFilter;
import pl.pingpong.club.config.SecurityConfig;
import pl.pingpong.club.config.TestSecurityConfig;
import pl.pingpong.club.dto.AuthResponse;
import pl.pingpong.club.dto.LoginRequest;
import pl.pingpong.club.dto.RegisterRequest;
import pl.pingpong.club.model.Role;
import pl.pingpong.club.service.AuthService;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
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
        AuthResponse response = new AuthResponse("jwt-token", "test@test.pl", Role.PLAYER,
                Instant.now().plusSeconds(3600));
        given(authService.login(any())).willReturn(response);

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
    void register_validRequest_returns201() throws Exception {
        AuthResponse response = new AuthResponse("jwt-token", "new@test.pl", Role.PLAYER,
                Instant.now().plusSeconds(3600));
        given(authService.register(any())).willReturn(response);

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Jan", "Kowalski", "new@test.pl", "Password1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PLAYER"));
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
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Jan", "K", "not-an-email", "Password1"))))
                .andExpect(status().isBadRequest());
    }
}
