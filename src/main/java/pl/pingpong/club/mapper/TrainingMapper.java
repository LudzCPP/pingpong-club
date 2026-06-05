package pl.pingpong.club.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.pingpong.club.dto.TrainingResponse;
import pl.pingpong.club.model.Training;

@Mapper(componentModel = "spring")
public interface TrainingMapper {

    @Mapping(target = "playerId",       source = "player.id")
    @Mapping(target = "playerFullName", expression = "java(t.getPlayer().getFirstName() + \" \" + t.getPlayer().getLastName())")
    @Mapping(target = "coachId",        source = "coach.id")
    @Mapping(target = "coachFullName",  expression = "java(t.getCoach().getFirstName() + \" \" + t.getCoach().getLastName())")
    @Mapping(target = "totalPrice", source = "totalPrice")
    TrainingResponse toResponse(Training t);
}
