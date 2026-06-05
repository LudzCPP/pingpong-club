package pl.pingpong.club.integration;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import pl.pingpong.club.dto.*;
import pl.pingpong.club.service.EmailService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrationTest {

    static final PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:16-alpine");
        postgres.start();
    }

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;
    @MockBean  EmailService emailService;

    // State shared across tests (instance fields — PER_CLASS lifecycle)
    String coachToken;
    String playerToken;
    UUID playerId;
    UUID trainingId;
    UUID trainingToCancel;

    @BeforeAll
    void setupSharedState() {
        rest.getRestTemplate().setRequestFactory(
                new HttpComponentsClientHttpRequestFactory(HttpClients.createDefault()));

        // Login as seeded coach
        var login = rest.postForEntity(url("/auth/login"),
                new LoginRequest("trener@pingpong.pl", "Coach123!"), AuthResponse.class);
        coachToken = login.getBody().token();

        // Generate invite link
        var invite = rest.exchange(url("/auth/invite"), POST,
                bearer(null, coachToken), InviteResponse.class);
        String inviteUrl = invite.getBody().inviteUrl();
        String inviteToken = inviteUrl.substring(inviteUrl.lastIndexOf('=') + 1);

        // Register player via invite token
        rest.postForEntity(url("/auth/register?token=" + inviteToken),
                new RegisterRequest("Marek", "Testowy", "marek@test.pl", "Player123!"),
                AuthResponse.class);

        // Login as player
        var playerLogin = rest.postForEntity(url("/auth/login"),
                new LoginRequest("marek@test.pl", "Player123!"), AuthResponse.class);
        playerToken = playerLogin.getBody().token();

        // Fetch player ID
        var profile = rest.exchange(url("/users/me"), GET,
                bearer(null, playerToken), UserResponse.class);
        playerId = profile.getBody().id();
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    @Test @Order(1)
    void login_validCredentials_returns200WithAdminRole() {
        var response = rest.postForEntity(url("/auth/login"),
                new LoginRequest("trener@pingpong.pl", "Coach123!"), AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().token()).isNotBlank();
        assertThat(response.getBody().role().name()).isEqualTo("ADMIN");
    }

    @Test @Order(2)
    void login_wrongPassword_returns401() {
        var response = rest.postForEntity(url("/auth/login"),
                new LoginRequest("trener@pingpong.pl", "wrongPass"), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test @Order(3)
    void generateInvite_asPlayer_returns403() {
        var response = rest.exchange(url("/auth/invite"), POST,
                bearer(null, playerToken), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test @Order(4)
    void register_usedToken_returns422() {
        // generate a fresh token, use it, then try to use it again
        var invite = rest.exchange(url("/auth/invite"), POST,
                bearer(null, coachToken), InviteResponse.class);
        String inviteUrl = invite.getBody().inviteUrl();
        String token = inviteUrl.substring(inviteUrl.lastIndexOf('=') + 1);

        rest.postForEntity(url("/auth/register?token=" + token),
                new RegisterRequest("First", "User", "first@test.pl", "Pass123!"),
                AuthResponse.class);

        var second = rest.postForEntity(url("/auth/register?token=" + token),
                new RegisterRequest("Second", "User", "second@test.pl", "Pass123!"),
                Object.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test @Order(5)
    void register_invalidToken_returns422() {
        var response = rest.postForEntity(url("/auth/register?token=nieistniejacy-token"),
                new RegisterRequest("Jan", "K", "jan@test.pl", "Pass123!"), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    @Test @Order(6)
    void getMyProfile_asPlayer_returns200WithOwnEmail() {
        var response = rest.exchange(url("/users/me"), GET,
                bearer(null, playerToken), UserResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().email()).isEqualTo("marek@test.pl");
    }

    @Test @Order(7)
    void getPlayers_asPlayer_returns403() {
        var response = rest.exchange(url("/users/players"), GET,
                bearer(null, playerToken), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test @Order(8)
    void getPlayers_asCoach_returns200() {
        var response = rest.exchange(url("/users/players"), GET,
                bearer(null, coachToken), UserResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
    }

    // ── Training lifecycle ────────────────────────────────────────────────────

    @Test @Order(9)
    void createTraining_asCoach_returns201WithCorrectName() {
        var body = new TrainingRequest(playerId, LocalDateTime.now().plusDays(1),
                60, new BigDecimal("120.00"), null);

        var response = rest.exchange(url("/trainings"), POST,
                bearer(body, coachToken), TrainingResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().status().name()).isEqualTo("SCHEDULED");
        assertThat(response.getBody().name()).isEqualTo("trening Marek");
        trainingId = response.getBody().id();
    }

    @Test @Order(10)
    void createTraining_asPlayer_returns403() {
        var body = new TrainingRequest(playerId, LocalDateTime.now().plusDays(3),
                60, new BigDecimal("100.00"), null);

        var response = rest.exchange(url("/trainings"), POST,
                bearer(body, playerToken), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test @Order(11)
    void createSecondTraining_forCancelTest() {
        var body = new TrainingRequest(playerId, LocalDateTime.now().plusDays(2),
                45, new BigDecimal("90.00"), null);

        var response = rest.exchange(url("/trainings"), POST,
                bearer(body, coachToken), TrainingResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        trainingToCancel = response.getBody().id();
    }

    @Test @Order(12)
    void listTrainings_asCoach_containsCreatedTrainings() {
        var response = rest.exchange(url("/trainings"), GET,
                bearer(null, coachToken), TrainingResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test @Order(13)
    void listTrainings_asPlayer_returnsOwnOnly() {
        var response = rest.exchange(url("/trainings"), GET,
                bearer(null, playerToken), TrainingResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test @Order(14)
    void completeTraining_returns200WithCompletedStatus() {
        var response = rest.exchange(url("/trainings/" + trainingId + "/complete"),
                PATCH, bearer(new CompleteTrainingRequest("Świetna sesja."), coachToken),
                TrainingResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status().name()).isEqualTo("COMPLETED");
        assertThat(response.getBody().notes()).isEqualTo("Świetna sesja.");
    }

    @Test @Order(15)
    void markAsPaid_returns200WithPaidTrue() {
        var response = rest.exchange(url("/trainings/" + trainingId + "/paid"),
                PATCH, bearer(null, coachToken), TrainingResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().paid()).isTrue();
    }

    @Test @Order(16)
    void cancelTraining_returns200WithCancelledStatus() {
        var response = rest.exchange(url("/trainings/" + trainingToCancel + "/cancel"),
                PATCH, bearer(null, coachToken), TrainingResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status().name()).isEqualTo("CANCELLED");
    }

    // ── Password change ───────────────────────────────────────────────────────

    @Test @Order(17)
    void changePassword_wrongCurrent_returns422() {
        var response = rest.exchange(url("/users/me/password"), PATCH,
                bearer(new ChangePasswordRequest("wrongPass", "NewPass123!"), playerToken),
                Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test @Order(18)
    void changePassword_returns204() {
        var response = rest.exchange(url("/users/me/password"), PATCH,
                bearer(new ChangePasswordRequest("Player123!", "NewPass123!"), playerToken),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test @Order(19)
    void loginWithNewPassword_returns200() {
        var response = rest.postForEntity(url("/auth/login"),
                new LoginRequest("marek@test.pl", "NewPass123!"), AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test @Order(20)
    void loginWithOldPassword_returns401() {
        var response = rest.postForEntity(url("/auth/login"),
                new LoginRequest("marek@test.pl", "Player123!"), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    private <T> HttpEntity<T> bearer(T body, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
