package com.echoreviews.mapper;

import com.echoreviews.dto.UserDTO;
import com.echoreviews.model.Album;
import com.echoreviews.model.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public User toEntity(UserDTO userDTO) {
        if (userDTO == null) {
            return null;
        }

        User user = new User();
        user.setId(userDTO.id());
        user.setUsername(userDTO.username());
        user.setPassword(userDTO.password());
        user.setEmail(userDTO.email());
        user.setAdmin(userDTO.isAdmin());
        user.setPotentiallyDangerous(userDTO.potentiallyDangerous());
        user.setBanned(userDTO.banned());
        user.setImageUrl(userDTO.imageUrl());
        user.setImageData(userDTO.imageData());
        user.setPdfPath(userDTO.pdfPath());
        
        if (userDTO.favoriteAlbumIds() != null) {
            List<Album> favoriteAlbums = userDTO.favoriteAlbumIds().stream()
                .map(this::mapAlbumIdToEntity)
                .collect(Collectors.toList());
            user.setFavoriteAlbums(favoriteAlbums);
        }
        
        if (userDTO.followers() != null) {
            user.setFollowers(new ArrayList<>(userDTO.followers()));
        }
        
        if (userDTO.following() != null) {
            user.setFollowing(new ArrayList<>(userDTO.following()));
        }
        
        return user;
    }

    public UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }

        List<Long> favoriteAlbumIds = null;
        if (user.getFavoriteAlbums() != null) {
            favoriteAlbumIds = user.getFavoriteAlbums().stream()
                .map(this::mapAlbumEntityToId)
                .collect(Collectors.toList());
        }

        return new UserDTO(
            user.getId(),
            user.getUsername(),
            null,
            user.getEmail(),
            user.isAdmin(),
            user.isPotentiallyDangerous(),
            user.isBanned(),
            user.getImageUrl(),
            user.getImageData(),
            user.getFollowers() != null ? new ArrayList<>(user.getFollowers()) : new ArrayList<>(),
            user.getFollowing() != null ? new ArrayList<>(user.getFollowing()) : new ArrayList<>(),
            favoriteAlbumIds != null ? favoriteAlbumIds : new ArrayList<>(),
            user.getPdfPath()
        );
    }

    public Album mapAlbumIdToEntity(Long albumId) {
        if (albumId == null) return null;
        Album album = new Album();
        album.setId(albumId);
        return album;
    }

    public Long mapAlbumEntityToId(Album album) {
        return album != null ? album.getId() : null;
    }

    public List<UserDTO> toDTOList(List<User> users) {
        if (users == null) {
            return new ArrayList<>();
        }
        
        return users.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public List<User> toEntityList(List<UserDTO> userDTOs) {
        if (userDTOs == null) {
            return new ArrayList<>();
        }
        
        return userDTOs.stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }
}