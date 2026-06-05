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
import pl.pingpong.club.dto.TrainingRequest;
import pl.pingpong.club.dto.TrainingResponse;
import pl.pingpong.club.model.TrainingStatus;
import pl.pingpong.club.service.AiParseService;
import pl.pingpong.club.service.TrainingService;
import pl.pingpong.club.service.UserService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = TrainingController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
        }
)
@Import(TestSecurityConfig.class)
class TrainingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean TrainingService trainingService;
    @MockBean UserService     userService;
    @MockBean AiParseService  aiParseService;

    @Test
    void getTrainings_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/trainings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void getTrainings_authenticated_returns200() throws Exception {
        given(trainingService.getTrainings(anyString())).willReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/trainings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("trening Anna"));
    }

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void createTraining_asCoach_returns201() throws Exception {
        given(trainingService.createTraining(any(), anyString())).willReturn(sampleResponse());

        mockMvc.perform(post("/trainings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTrainingRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("trening Anna"));
    }

    @Test
    @WithMockUser(username = "player@test.pl", roles = "PLAYER")
    void createTraining_asPlayer_returns403() throws Exception {
        mockMvc.perform(post("/trainings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTrainingRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void cancelTraining_asCoach_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        given(trainingService.cancelTraining(id)).willReturn(sampleResponse());

        mockMvc.perform(patch("/trainings/{id}/cancel", id).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "player@test.pl", roles = "PLAYER")
    void cancelTraining_asPlayer_returns403() throws Exception {
        mockMvc.perform(patch("/trainings/{id}/cancel", UUID.randomUUID()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void completeTraining_asCoach_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        given(trainingService.completeTraining(eq(id), any())).willReturn(sampleResponse());

        mockMvc.perform(patch("/trainings/{id}/complete", id).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void updateTraining_asCoach_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        given(trainingService.updateTraining(any(), any(), anyString())).willReturn(sampleResponse());

        mockMvc.perform(put("/trainings/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTrainingRequest())))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "player@test.pl", roles = "PLAYER")
    void updateTraining_asPlayer_returns403() throws Exception {
        mockMvc.perform(put("/trainings/{id}", UUID.randomUUID())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTrainingRequest())))
                .andExpect(status().isForbidden());
    }

    private TrainingResponse sampleResponse() {
        return new TrainingResponse(
                UUID.randomUUID(), "trening Anna",
                UUID.randomUUID(), "Anna Kowalska",
                UUID.randomUUID(), "Piotr Trener",
                LocalDateTime.now().plusDays(1), 60,
                TrainingStatus.SCHEDULED,
                BigDecimal.valueOf(100), BigDecimal.valueOf(100), null, false
        );
    }

    private TrainingRequest validTrainingRequest() {
        return new TrainingRequest(
                UUID.randomUUID(),
                LocalDateTime.of(2030, 1, 1, 10, 0),
                60,
                BigDecimal.valueOf(100),
                null
        );
    }
}
