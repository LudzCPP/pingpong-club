package pl.pingpong.club.mapper;

import org.mapstruct.Mapper;
import pl.pingpong.club.dto.UserResponse;
import pl.pingpong.club.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}
