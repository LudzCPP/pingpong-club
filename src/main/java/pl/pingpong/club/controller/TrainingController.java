package pl.pingpong.club.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pl.pingpong.club.dto.CompleteTrainingRequest;
import pl.pingpong.club.dto.TrainingParseRequest;
import pl.pingpong.club.dto.TrainingParseResponse;
import pl.pingpong.club.dto.TrainingRequest;
import pl.pingpong.club.dto.TrainingResponse;
import pl.pingpong.club.service.AiParseService;
import pl.pingpong.club.service.TrainingService;
import pl.pingpong.club.service.UserService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/trainings")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingService trainingService;
    private final UserService     userService;
    private final AiParseService  aiParseService;

    /**
     * GET /api/trainings
     * COACH: wszystkie treningi | PLAYER: tylko własne
     */
    @GetMapping
    public List<TrainingResponse> getTrainings(@AuthenticationPrincipal UserDetails user) {
        return trainingService.getTrainings(user.getUsername());
    }

    /**
     * GET /api/trainings/{id}
     * COACH: każdy trening | PLAYER: tylko własny (404 jeśli cudzy)
     */
    @GetMapping("/{id}")
    public TrainingResponse getTraining(@PathVariable UUID id,
                                        @AuthenticationPrincipal UserDetails user) {
        return trainingService.getTrainingById(id, user.getUsername());
    }

    /**
     * POST /api/trainings
     * Tylko COACH. Nazwa "trening [Imię]" generowana automatycznie.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COACH')")
    public TrainingResponse createTraining(@Valid @RequestBody TrainingRequest request,
                                           @AuthenticationPrincipal UserDetails user) {
        return trainingService.createTraining(request, user.getUsername());
    }

    /**
     * PUT /api/trainings/{id}
     * Tylko COACH. Trening musi być w stanie SCHEDULED.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COACH')")
    public TrainingResponse updateTraining(@PathVariable UUID id,
                                           @Valid @RequestBody TrainingRequest request,
                                           @AuthenticationPrincipal UserDetails user) {
        return trainingService.updateTraining(id, request, user.getUsername());
    }

    /**
     * PATCH /api/trainings/{id}/cancel
     * Tylko COACH. Odwołuje trening (zmienia status na CANCELLED).
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('COACH')")
    public TrainingResponse cancelTraining(@PathVariable UUID id) {
        return trainingService.cancelTraining(id);
    }

    /**
     * PATCH /api/trainings/{id}/complete
     * Tylko COACH. Oznacza trening jako zrealizowany → wchodzi do rozliczeń finansowych.
     */
    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('COACH')")
    public TrainingResponse completeTraining(@PathVariable UUID id,
                                             @RequestBody(required = false) CompleteTrainingRequest request) {
        return trainingService.completeTraining(id, request);
    }

    @PatchMapping("/{id}/paid")
    @PreAuthorize("hasRole('COACH')")
    public TrainingResponse togglePaid(@PathVariable UUID id) {
        return trainingService.markPaid(id);
    }

    /**
     * POST /api/trainings/parse
     * Tylko COACH. Parsuje tekst/dyktowanie przez Claude i zwraca pre-wypełnione pola formularza.
     */
    @PostMapping("/parse")
    @PreAuthorize("hasRole('COACH')")
    public TrainingParseResponse parseTraining(@Valid @RequestBody TrainingParseRequest request,
                                               @AuthenticationPrincipal UserDetails user) {
        return aiParseService.parseTrainingText(request.text(), userService.getAllPlayers(user.getUsername()));
    }
}
