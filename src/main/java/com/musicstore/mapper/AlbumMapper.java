package com.musicstore.mapper;

import com.musicstore.dto.AlbumDTO;
import com.musicstore.model.Album;
import com.musicstore.model.Artist;
import com.musicstore.model.Review;
import com.musicstore.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface AlbumMapper {
    AlbumDTO toDTO(Album album);
    Album toEntity(AlbumDTO albumDTO);
    List<AlbumDTO> toDTOList(List<Album> albums);
    List<Album> toEntityList(List<AlbumDTO> albumDTOs);
    public default List<String> mapUsersToUsernames(List<User> users) {
        if (users == null) {
            return null;
        }
        return users.stream()
                .map(User::getUsername)  // Extrae el username de cada User
                .collect(Collectors.toList());
    }
}