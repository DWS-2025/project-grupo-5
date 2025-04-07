package com.musicstore.dto;

import com.musicstore.model.User;
import java.util.List;
import java.util.stream.Collectors;

public record UserDTO(
    Long id,
    String username,
    String email,
    boolean isAdmin,
    String imageUrl,
    List<Long> followers,
    List<Long> following,
    List<Long> favoriteAlbumIds
) {
    public static UserDTO fromUser(User user) {
        return new UserDTO(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.isAdmin(),
            user.getImageUrl(),
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
        user.setFollowers(this.followers());
        user.setFollowing(this.following());
        return user;
    }
}