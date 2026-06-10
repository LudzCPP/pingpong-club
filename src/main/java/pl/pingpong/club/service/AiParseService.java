package pl.pingpong.club.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pl.pingpong.club.dto.TrainingParseResponse;
import pl.pingpong.club.dto.UserResponse;
import pl.pingpong.club.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Parsuje tekst/dyktowanie trenera za pomocą dowolnego modelu zgodnego z OpenAI Chat API.
 *
 * Domyślna konfiguracja używa Groq (darmowy tier, ~14 000 req/dzień).
 * Zmiana providera = edycja application.yml (ai.base-url + ai.model).
 *
 * Przykłady:
 *   Groq:   base-url: https://api.groq.com/openai/v1   model: llama-3.3-70b-versatile
 *   OpenAI: base-url: https://api.openai.com/v1        model: gpt-4o-mini
 *   Ollama: base-url: http://localhost:11434/v1         model: llama3.2   api-key: ollama
 */
@Service
@RequiredArgsConstructor
public class AiParseService {

    @Value("${ai.base-url}")
    private String baseUrl;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.model}")
    private String model;

    private final ObjectMapper objectMapper;
    private final HttpClient   httpClient = HttpClient.newHttpClient();

    public TrainingParseResponse parseTrainingText(String text, List<UserResponse> players) {
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessRuleException(
                "Klucz AI API nie jest skonfigurowany. Ustaw zmienną środowiskową AI_API_KEY."
            );
        }
        String responseText = callApi(buildPrompt(text, players));
        return parseResponse(responseText, players);
    }

    // ── prompt ─────────────────────────────────────────────────────────────────

    private String buildPrompt(String text, List<UserResponse> players) {
        LocalDate today = LocalDate.now();
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        String playerList = players.stream()
                .map(p -> p.id() + ": " + p.firstName() + " " + p.lastName())
                .collect(Collectors.joining("\n"));

        // Pre-obliczone daty — LLM nie musi liczyć dni tygodnia sam
        String calendar = buildDayCalendar(today);

        return """
                Jesteś asystentem systemu zarządzania klubem tenisa stołowego.
                Trener opisał trening głosowo lub tekstowo po polsku.

                Aktualna data i godzina: %s

                Nadchodzące daty dni tygodnia (użyj DOKŁADNIE tych dat, nie licz sam):
                %s

                Zawodnicy w systemie (UUID: imię nazwisko):
                %s

                Sparsuj poniższy tekst i zwróć WYŁĄCZNIE JSON (bez markdown, bez objaśnień):
                {
                  "playerId": "<UUID zawodnika lub null>",
                  "playerName": "<dopasowane imię i nazwisko lub null>",
                  "scheduledAt": "<data i czas ISO-8601, np. 2026-06-06T16:00:00, lub null>",
                  "durationMinutes": <liczba całkowita lub null>,
                  "totalPrice": <łączna kwota za cały trening w zł lub null>,
                  "notes": "<notatki lub pusty string>"
                }

                Zasady dopasowania:
                - Dopasowuj zawodników po imieniu, nazwisku lub ich części (z błędami pisowni też)
                - Daty: "jutro", "pojutrze", "w środę", "za 3 dni" — użyj tabeli dat powyżej
                - "w wtorek" lub "najbliższy wtorek" = wiersz "wtorek (najbliższy)" z tabeli
                - "wtorek za tydzień" lub "następny wtorek" = wiersz "wtorek za tydzień" z tabeli
                - "za N dni" = data bieżąca + N (policz od "Aktualna data i godzina" powyżej)
                - Godziny: "o 16" → 16:00, "o piętnastej trzydzieści" → 15:30, "o wpół do czwartej" → 15:30
                - Czas trwania: "godzina" → 60, "półtorej" → 90, "45 minut" → 45, "pół godziny" → 30
                - Kwoty (łączna cena treningu): "sto dwadzieścia złotych" → 120, "150 zł" → 150, "stówa" → 100
                - Jeśli informacja nie pada w tekście, wstaw null

                Tekst do sparsowania: "%s"
                """.formatted(now, calendar, playerList, text.replace("\"", "\\\""));
    }

    private String buildDayCalendar(LocalDate today) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<DayOfWeek, String> polishNames = Map.of(
                DayOfWeek.MONDAY,    "poniedziałek",
                DayOfWeek.TUESDAY,   "wtorek",
                DayOfWeek.WEDNESDAY, "środa",
                DayOfWeek.THURSDAY,  "czwartek",
                DayOfWeek.FRIDAY,    "piątek",
                DayOfWeek.SATURDAY,  "sobota",
                DayOfWeek.SUNDAY,    "niedziela"
        );
        DayOfWeek[] order = {
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        };

        StringBuilder sb = new StringBuilder();
        sb.append("- jutro: ").append(today.plusDays(1).format(fmt)).append("\n");
        sb.append("- pojutrze: ").append(today.plusDays(2).format(fmt)).append("\n");
        for (DayOfWeek dow : order) {
            LocalDate nearest = today.with(TemporalAdjusters.nextOrSame(dow));
            if (!nearest.isAfter(today)) nearest = nearest.plusWeeks(1);
            String name = polishNames.get(dow);
            sb.append("- ").append(name).append(" (najbliższy): ").append(nearest.format(fmt)).append("\n");
            sb.append("- ").append(name).append(" za tydzień: ").append(nearest.plusWeeks(1).format(fmt)).append("\n");
        }
        return sb.toString().stripTrailing();
    }

    // ── HTTP call (OpenAI-compatible format) ────────────────────────────────────

    private String callApi(String prompt) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "max_tokens", 400,
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new BusinessRuleException("Błąd AI API (HTTP " + response.statusCode() + "): " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessRuleException("Błąd połączenia z AI API: " + e.getMessage());
        }
    }

    // ── response parsing ────────────────────────────────────────────────────────

    private TrainingParseResponse parseResponse(String json, List<UserResponse> players) {
        try {
            String cleaned = json.replaceAll("(?s)```json\\s*|```\\s*", "").trim();
            JsonNode node = objectMapper.readTree(cleaned);

            UUID   playerId   = resolvePlayer(node, players);
            String playerName = resolvePlayerName(playerId, node, players);

            LocalDateTime scheduledAt = parseDateTime(node.path("scheduledAt").asText(null));
            Integer       duration    = parseInteger(node.path("durationMinutes"));
            BigDecimal    totalPrice  = parseDecimal(node.path("totalPrice"));
            String        notes       = node.path("notes").asText("");

            return new TrainingParseResponse(playerId, playerName, scheduledAt, duration, totalPrice, notes);

        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessRuleException("Nie udało się sparsować odpowiedzi modelu: " + e.getMessage());
        }
    }

    private UUID resolvePlayer(JsonNode node, List<UserResponse> players) {
        String idStr = node.path("playerId").asText(null);
        if (idStr != null && !idStr.equals("null") && !idStr.isBlank()) {
            try {
                UUID uuid = UUID.fromString(idStr);
                if (players.stream().anyMatch(p -> p.id().equals(uuid))) return uuid;
            } catch (IllegalArgumentException ignored) {}
        }
        String name = node.path("playerName").asText(null);
        if (name == null || name.equals("null") || name.isBlank()) return null;
        String nameLower = name.toLowerCase();
        return players.stream()
                .filter(p -> {
                    String full = (p.firstName() + " " + p.lastName()).toLowerCase();
                    return full.contains(nameLower) || nameLower.contains(p.firstName().toLowerCase());
                })
                .map(UserResponse::id)
                .findFirst()
                .orElse(null);
    }

    private String resolvePlayerName(UUID playerId, JsonNode node, List<UserResponse> players) {
        if (playerId != null) {
            return players.stream()
                    .filter(p -> p.id().equals(playerId))
                    .map(p -> p.firstName() + " " + p.lastName())
                    .findFirst().orElse(null);
        }
        String name = node.path("playerName").asText(null);
        return (name != null && !name.equals("null")) ? name : null;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.equals("null") || value.isBlank()) return null;
        try { return LocalDateTime.parse(value); } catch (Exception ignored) { return null; }
    }

    private Integer parseInteger(JsonNode node) {
        return (!node.isNull() && node.isNumber()) ? node.asInt() : null;
    }

    private BigDecimal parseDecimal(JsonNode node) {
        return (!node.isNull() && node.isNumber()) ? node.decimalValue() : null;
    }
}
