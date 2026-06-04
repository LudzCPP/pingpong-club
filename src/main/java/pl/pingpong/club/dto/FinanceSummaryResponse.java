package pl.pingpong.club.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FinanceSummaryResponse(
        LocalDate from,
        LocalDate to,

        /** Łączna kwota za zrealizowane treningi w przedziale. */
        BigDecimal totalTrainingEarnings,

        /** Łączna kwota wypłat za mecze ligowe w przedziale. */
        BigDecimal totalMatchPayments,

        /** Suma zarobków (treningi + mecze). */
        BigDecimal grandTotal,

        int completedTrainingsCount,
        int matchesCount,

        List<TrainingResponse> trainings,
        List<LeagueMatchResponse> matches
) {}
