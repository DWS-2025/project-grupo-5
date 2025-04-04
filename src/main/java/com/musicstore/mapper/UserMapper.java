package com.musicstore.mapper;

import com.musicstore.dto.UserDTO;
import com.musicstore.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "favoriteAlbumIds", expression = "java(user.getFavoriteAlbums().stream().map(album -> album.getId()).collect(java.util.stream.Collectors.toList()))")
    UserDTO toDTO(User user);

    @Mapping(target = "favoriteAlbums", ignore = true)
    @Mapping(target = "password", ignore = true)
    User toEntity(UserDTO userDTO);

    List<UserDTO> toDTOList(List<User> users);
    List<User> toEntityList(List<UserDTO> userDTOs);
}