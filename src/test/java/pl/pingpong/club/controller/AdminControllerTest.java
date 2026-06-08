package pl.pingpong.club.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pl.pingpong.club.config.JwtAuthFilter;
import pl.pingpong.club.config.SecurityConfig;
import pl.pingpong.club.config.TestSecurityConfig;
import pl.pingpong.club.dto.AdminStatsResponse;
import pl.pingpong.club.service.AdminService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = AdminController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
        }
)
@Import(TestSecurityConfig.class)
class AdminControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean AdminService adminService;

    @Test
    @WithMockUser(username = "admin@test.pl", roles = "ADMIN")
    void getStats_asAdmin_returns200WithStats() throws Exception {
        given(adminService.getStats()).willReturn(sampleStats());

        mockMvc.perform(get("/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCoaches").value(2))
                .andExpect(jsonPath("$.activeCoaches").value(2))
                .andExpect(jsonPath("$.totalPlayers").value(5))
                .andExpect(jsonPath("$.activePlayers").value(4))
                .andExpect(jsonPath("$.scheduledTrainings").value(3))
                .andExpect(jsonPath("$.completedTrainings").value(10))
                .andExpect(jsonPath("$.cancelledTrainings").value(1))
                .andExpect(jsonPath("$.totalEarnings").value(1500))
                .andExpect(jsonPath("$.coachStats").isArray())
                .andExpect(jsonPath("$.coachStats[0].playerCount").value(3));
    }

    @Test
    @WithMockUser(username = "coach@test.pl", roles = "COACH")
    void getStats_asCoach_returns403() throws Exception {
        mockMvc.perform(get("/admin/stats"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "player@test.pl", roles = "PLAYER")
    void getStats_asPlayer_returns403() throws Exception {
        mockMvc.perform(get("/admin/stats"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getStats_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/admin/stats"))
                .andExpect(status().isForbidden());
    }

    private AdminStatsResponse sampleStats() {
        var coachEntry = new AdminStatsResponse.CoachStatEntry(
                UUID.randomUUID(), "Piotr", "Trener", "coach@test.pl", true, 3
        );
        return new AdminStatsResponse(2, 2, 5, 4, 3, 10, 1, BigDecimal.valueOf(1500), List.of(coachEntry));
    }
}
