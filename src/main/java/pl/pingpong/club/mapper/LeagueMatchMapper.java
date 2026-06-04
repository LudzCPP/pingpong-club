package pl.pingpong.club.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.pingpong.club.dto.LeagueMatchResponse;
import pl.pingpong.club.model.LeagueMatch;

@Mapper(componentModel = "spring")
public interface LeagueMatchMapper {

    @Mapping(target = "playerId",       source = "player.id")
    @Mapping(target = "playerFullName", expression = "java(m.getPlayer().getFirstName() + \" \" + m.getPlayer().getLastName())")
    LeagueMatchResponse toResponse(LeagueMatch m);
}
