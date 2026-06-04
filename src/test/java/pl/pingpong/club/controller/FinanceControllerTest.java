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
import pl.pingpong.club.dto.FinanceSummaryResponse;
import pl.pingpong.club.dto.LeagueMatchRequest;
import pl.pingpong.club.dto.LeagueMatchResponse;
import pl.pingpong.club.service.FinanceService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = FinanceController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
        }
)
@Import(TestSecurityConfig.class)
class FinanceControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean FinanceService financeService;

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void getSummary_asCoach_returns200() throws Exception {
        given(financeService.getSummary(any(), any(), any(), anyString()))
                .willReturn(emptySummary());

        mockMvc.perform(get("/finances/summary")
                        .param("from", "2024-01-01")
                        .param("to", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedTrainingsCount").value(0));
    }

    @Test
    @WithMockUser(username = "player@test.pl", roles = "PLAYER")
    void getMatches_asPlayer_returns200() throws Exception {
        given(financeService.getMatches(anyString())).willReturn(List.of(sampleMatch()));

        mockMvc.perform(get("/finances/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].result").value("3:1"));
    }

    @Test
    void getMatches_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/finances/matches"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void createMatch_asCoach_returns201() throws Exception {
        given(financeService.createMatch(any())).willReturn(sampleMatch());

        mockMvc.perform(post("/finances/matches")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validMatchRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result").value("3:1"));
    }

    @Test
    @WithMockUser(username = "player@test.pl", roles = "PLAYER")
    void createMatch_asPlayer_returns403() throws Exception {
        mockMvc.perform(post("/finances/matches")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validMatchRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void deleteMatch_asCoach_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        willDoNothing().given(financeService).deleteMatch(id);

        mockMvc.perform(delete("/finances/matches/{id}", id).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "player@test.pl", roles = "PLAYER")
    void deleteMatch_asPlayer_returns403() throws Exception {
        mockMvc.perform(delete("/finances/matches/{id}", UUID.randomUUID()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void createMatch_invalidResult_returns400() throws Exception {
        LeagueMatchRequest badRequest = new LeagueMatchRequest(
                UUID.randomUUID(), LocalDate.of(2024, 1, 1), "Club XYZ",
                "invalid-result", BigDecimal.valueOf(50), null
        );

        mockMvc.perform(post("/finances/matches")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }

    private FinanceSummaryResponse emptySummary() {
        return new FinanceSummaryResponse(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                0, 0, List.of(), List.of()
        );
    }

    private LeagueMatchResponse sampleMatch() {
        return new LeagueMatchResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Anna Kowalska",
                LocalDate.of(2024, 1, 15), "Club XYZ", "3:1",
                BigDecimal.valueOf(50), null
        );
    }

    private LeagueMatchRequest validMatchRequest() {
        return new LeagueMatchRequest(
                UUID.randomUUID(),
                LocalDate.of(2024, 1, 15),
                "Club XYZ",
                "3:1",
                BigDecimal.valueOf(50),
                null
        );
    }
}
