package pl.pingpong.club.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.pingpong.club.dto.TrainingPackageResponse;
import pl.pingpong.club.model.TrainingPackage;

@Mapper(componentModel = "spring")
public interface TrainingPackageMapper {

    @Mapping(target = "playerId",       source = "player.id")
    @Mapping(target = "playerFullName", expression = "java(p.getPlayer().getFirstName() + \" \" + p.getPlayer().getLastName())")
    @Mapping(target = "coachId",        source = "coach.id")
    @Mapping(target = "coachFullName",  expression = "java(p.getCoach().getFirstName() + \" \" + p.getCoach().getLastName())")
    TrainingPackageResponse toResponse(TrainingPackage p);
}
