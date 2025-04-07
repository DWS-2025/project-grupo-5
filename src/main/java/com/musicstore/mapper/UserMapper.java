package com.musicstore.mapper;

import com.musicstore.dto.UserDTO;
import com.musicstore.model.Album;
import com.musicstore.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Qualifier
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.METHOD)
    static @interface AlbumIdToEntity {}

    @Qualifier
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.METHOD)
    static @interface AlbumEntityToId {}
    @Mapping(target = "favoriteAlbums", source = "favoriteAlbumIds", qualifiedBy = AlbumIdToEntity.class)
    User toEntity(UserDTO userDTO);

    @Mapping(target = "favoriteAlbumIds", source = "favoriteAlbums", qualifiedBy = AlbumEntityToId.class)
    @Mapping(target = "followers", source = "followers")
    @Mapping(target = "following", source = "following")
    UserDTO toDTO(User user);

    @AlbumIdToEntity
    default Album mapAlbumIdToEntity(Long albumId) {
        if (albumId == null) return null;
        Album album = new Album();
        album.setId(albumId);
        return album;
    }

    @AlbumEntityToId
    default Long mapAlbumEntityToId(Album album) {
        return album != null ? album.getId() : null;
    }

    List<UserDTO> toDTOList(List<User> users);
    List<User> toEntityList(List<UserDTO> userDTOs);
}