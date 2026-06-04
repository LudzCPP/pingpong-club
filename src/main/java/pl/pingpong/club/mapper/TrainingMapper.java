package pl.pingpong.club.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.pingpong.club.dto.TrainingResponse;
import pl.pingpong.club.model.Training;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Mapper(componentModel = "spring")
public interface TrainingMapper {

    @Mapping(target = "playerId",       source = "player.id")
    @Mapping(target = "playerFullName", expression = "java(t.getPlayer().getFirstName() + \" \" + t.getPlayer().getLastName())")
    @Mapping(target = "coachId",        source = "coach.id")
    @Mapping(target = "coachFullName",  expression = "java(t.getCoach().getFirstName() + \" \" + t.getCoach().getLastName())")
    @Mapping(target = "totalPrice",     expression = "java(calcTotal(t))")
    TrainingResponse toResponse(Training t);

    default BigDecimal calcTotal(Training t) {
        return t.getHourlyRate()
                .multiply(BigDecimal.valueOf(t.getDurationMinutes()))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
}
