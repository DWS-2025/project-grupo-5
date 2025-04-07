package com.musicstore.dto;

import com.musicstore.model.User;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public record UserDTO(
    Long id,
    String username,
    String password,
    String email,
    boolean isAdmin,
    String imageUrl,
    byte[] imageData,
    List<Long> followers,
    List<Long> following,
    List<Long> favoriteAlbumIds
) {
    public UserDTO {
        followers = followers != null ? followers : new ArrayList<>();
        following = following != null ? following : new ArrayList<>();
        favoriteAlbumIds = favoriteAlbumIds != null ? favoriteAlbumIds : new ArrayList<>();
    }
    public static UserDTO fromUser(User user) {
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
                .map(album -> album.getId())
                .collect(Collectors.toList())
        );
    }

    public User toUser() {
        User user = new User();
        user.setId(this.id());
        user.setUsername(this.username());
        user.setEmail(this.email());
        user.setAdmin(this.isAdmin());
        user.setImageUrl(this.imageUrl());
        user.setImageData(this.imageData());
        user.setFollowers(this.followers());
        user.setFollowing(this.following());
        return user;
    }

    public UserDTO withId(Long newId) {
        return new UserDTO(
            newId,
            this.username(),
            this.password(),
            this.email(),
            this.isAdmin(),
            this.imageUrl(),
            this.imageData(),
            this.followers(),
            this.following(),
            this.favoriteAlbumIds
        );
    }

    public UserDTO withImageData(byte[] newImageData) {
        return new UserDTO(
            this.id(),
            this.username(),
            this.password(),
            this.email(),
            this.isAdmin(),
            this.imageUrl(),
            newImageData,
            this.followers(),
            this.following(),
            this.favoriteAlbumIds
        );
    }

    public UserDTO withImageUrl(String newImageUrl) {
        return new UserDTO(
            this.id(),
            this.username(),
            this.password(),
            this.email(),
            this.isAdmin(),
            newImageUrl,
            this.imageData(),
            this.followers(),
            this.following(),
            this.favoriteAlbumIds
        );
    }

    public UserDTO withFavoriteAlbumIds(List<Long> newFavoriteAlbumIds) {
        return new UserDTO(
            this.id(),
            this.username(),
            this.password(),
            this.email(),
            this.isAdmin(),
            this.imageUrl(),
            this.imageData(),
            this.followers(),
            this.following(),
            newFavoriteAlbumIds
        );
    }

    public UserDTO withFollowing(List<Long> newFollowing) {
        return new UserDTO(
            this.id(),
            this.username(),
            this.password(),
            this.email(),
            this.isAdmin(),
            this.imageUrl(),
            this.imageData(),
            this.followers(),
            newFollowing,
            this.favoriteAlbumIds
        );
    }

    public UserDTO withFollowers(List<Long> newFollowers) {
        return new UserDTO(
            this.id(),
            this.username(),
            this.password(),
            this.email(),
            this.isAdmin(),
            this.imageUrl(),
            this.imageData(),
            newFollowers,
            this.following(),
            this.favoriteAlbumIds
        );
    }
}