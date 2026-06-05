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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnthropicService {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL   = "claude-haiku-4-5-20251001";

    @Value("${anthropic.api-key:}")
    private String apiKey;

    private final ObjectMapper objectMapper;
    private final HttpClient   httpClient = HttpClient.newHttpClient();

    public TrainingParseResponse parseTrainingText(String text, List<UserResponse> players) {
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessRuleException(
                "Klucz API Anthropic nie jest skonfigurowany. Ustaw zmienną środowiskową ANTHROPIC_API_KEY."
            );
        }
        String claudeJson = callApi(buildPrompt(text, players));
        return parseResponse(claudeJson, players);
    }

    // ── prompt ─────────────────────────────────────────────────────────────────

    private String buildPrompt(String text, List<UserResponse> players) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        String playerList = players.stream()
                .map(p -> p.id() + ": " + p.firstName() + " " + p.lastName())
                .collect(Collectors.joining("\n"));

        return """
                Jesteś asystentem systemu zarządzania klubem tenisa stołowego.
                Trener opisał trening głosowo lub tekstowo po polsku.

                Aktualna data i godzina: %s

                Zawodnicy w systemie (UUID: imię nazwisko):
                %s

                Sparsuj poniższy tekst i zwróć WYŁĄCZNIE JSON (bez markdown, bez objaśnień):
                {
                  "playerId": "<UUID zawodnika lub null>",
                  "playerName": "<dopasowane imię i nazwisko lub null>",
                  "scheduledAt": "<data i czas ISO-8601, np. 2026-06-06T16:00:00, lub null>",
                  "durationMinutes": <liczba całkowita lub null>,
                  "hourlyRate": <liczba lub null>,
                  "notes": "<notatki lub pusty string>"
                }

                Zasady dopasowania:
                - Dopasowuj zawodników po imieniu, nazwisku lub ich części (z błędami pisowni też)
                - Daty: "jutro", "pojutrze", "w środę", "za 3 dni" — oblicz względem aktualnej daty
                - Godziny: "o 16" → 16:00, "o piętnastej trzydzieści" → 15:30, "o wpół do czwartej" → 15:30
                - Czas trwania: "godzina" → 60, "półtorej" → 90, "45 minut" → 45, "pół godziny" → 30
                - Kwoty: "sto dwadzieścia złotych" → 120, "150 zł" → 150, "stówa" → 100
                - Jeśli informacja nie pada w tekście, wstaw null

                Tekst do sparsowania: "%s"
                """.formatted(now, playerList, text.replace("\"", "\\\""));
    }

    // ── HTTP call ───────────────────────────────────────────────────────────────

    private String callApi(String prompt) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", MODEL,
                    "max_tokens", 400,
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new BusinessRuleException("Błąd API Anthropic (HTTP " + response.statusCode() + ").");
            }

            JsonNode root = objectMapper.readTree(response.body());
            return root.path("content").get(0).path("text").asText();

        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessRuleException("Błąd połączenia z API Anthropic: " + e.getMessage());
        }
    }

    // ── response parsing ────────────────────────────────────────────────────────

    private TrainingParseResponse parseResponse(String json, List<UserResponse> players) {
        try {
            // Strip markdown fences if model added them anyway
            String cleaned = json.replaceAll("(?s)```json\\s*|```\\s*", "").trim();
            JsonNode node = objectMapper.readTree(cleaned);

            UUID   playerId   = resolvePlayer(node, players);
            String playerName = resolvePlayerName(playerId, node, players);

            LocalDateTime scheduledAt  = parseDateTime(node.path("scheduledAt").asText(null));
            Integer       duration     = parseInteger(node.path("durationMinutes"));
            BigDecimal    hourlyRate   = parseDecimal(node.path("hourlyRate"));
            String        notes        = node.path("notes").asText("");

            return new TrainingParseResponse(playerId, playerName, scheduledAt, duration, hourlyRate, notes);

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
                boolean exists = players.stream().anyMatch(p -> p.id().equals(uuid));
                if (exists) return uuid;
            } catch (IllegalArgumentException ignored) {}
        }

        // Fallback: match by playerName field
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
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer parseInteger(JsonNode node) {
        return (!node.isNull() && node.isNumber()) ? node.asInt() : null;
    }

    private BigDecimal parseDecimal(JsonNode node) {
        return (!node.isNull() && node.isNumber()) ? node.decimalValue() : null;
    }
}
