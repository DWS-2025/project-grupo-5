package com.musicstore.mapper;

import com.musicstore.dto.UserDTO;
import com.musicstore.model.Album;
import com.musicstore.model.User;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }
        return new UserDTO(
            user.getId(),
            user.getUsername(),
            user.getPassword(),
            user.getEmail(),
            user.isAdmin(),
            user.getImageUrl(),
            user.getImageData(),
            user.getFollowers(),
            user.getFollowing(),
            user.getFavoriteAlbums().stream()
                .map(Album::getId)
                .collect(Collectors.toList())
        );
    }

    public User toEntity(UserDTO dto) {
        if (dto == null) {
            return null;
        }
        User user = new User();
        user.setId(dto.id());
        user.setUsername(dto.username());
        user.setPassword(dto.password());
        user.setEmail(dto.email());
        user.setAdmin(dto.isAdmin());
        user.setImageUrl(dto.imageUrl());
        user.setImageData(dto.imageData());
        user.setFollowers(dto.followers());
        user.setFollowing(dto.following());
        
        if (dto.favoriteAlbumIds() != null) {
            user.setFavoriteAlbums(
                dto.favoriteAlbumIds().stream()
                    .map(albumId -> {
                        Album album = new Album();
                        album.setId(albumId);
                        return album;
                    })
                    .collect(Collectors.toList())
            );
        }
        
        return user;
    }

    public List<UserDTO> toDTOList(List<User> users) {
        if (users == null) {
            return null;
        }
        return users.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public List<User> toEntityList(List<UserDTO> userDTOs) {
        if (userDTOs == null) {
            return null;
        }
        return userDTOs.stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }
}