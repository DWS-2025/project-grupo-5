package com.echoreviews.dto;

import com.echoreviews.model.Album;
import com.echoreviews.model.User;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

public record UserDTO(
        Long id,
        String username,
        String password,
        String email,
        boolean isAdmin,
        boolean potentiallyDangerous,
        boolean banned,
        String imageUrl,
        byte[] imageData,
        List<Long> followers,
        List <Long> following,
        List<Long> favoriteAlbumIds,
        String pdfPath
) {
    // Constructor alternativo con pdfPath con valor por defecto
    public UserDTO(
            Long id,
            String username,
            String password,
            String email,
            boolean isAdmin,
            boolean potentiallyDangerous,
            boolean banned,
            String imageUrl,
            byte[] imageData,
            List<Long> followers,
            List<Long> following,
            List<Long> favoriteAlbumIds) {
        this(id, username, password, email, isAdmin, potentiallyDangerous, banned, 
             imageUrl, imageData, followers, following, favoriteAlbumIds, null);
    }
    
    // Método seguro para comprobar si el usuario tiene un PDF
    public boolean hasPdf() {
        return pdfPath != null && !pdfPath.isEmpty();
    }
    
    // Método seguro para obtener pdfPath o un valor por defecto
    public String safePdfPath() {
        return pdfPath != null ? pdfPath : "";
    }
    
    public static UserDTO fromUser(User user) {
        return new UserDTO(
            user.getId(),
            user.getUsername(),
            user.getPassword(),
            user.getEmail(),
            user.isAdmin(),
            user.isPotentiallyDangerous(),
            user.isBanned(),
            user.getImageUrl(),
            user.getImageData(),
            user.getFollowers(),
            user.getFollowing(),
            user.getFavoriteAlbums().stream()
                .map(Album::getId)
                .collect(Collectors.toList()),
            user.getPdfPath()
        );
    }

    public User toUser() {
        User user = new User();
        user.setId(this.id());
        user.setUsername(this.username());
        user.setEmail(this.email());
        user.setAdmin(this.isAdmin());
        user.setPotentiallyDangerous(this.potentiallyDangerous());
        user.setBanned(this.banned());
        user.setImageUrl(this.imageUrl());
        user.setImageData(this.imageData());
        user.setFollowers(this.followers());
        user.setFollowing(this.following());

        
        if(this.favoriteAlbumIds != null) {
            user.setFavoriteAlbums(
                this.favoriteAlbumIds.stream()
                    .map(id -> {
                        Album album = new Album();
                        album.setId(id);
                        album.getFavoriteUsers().add(user);
                        return album;
                    })
                    .collect(Collectors.toList())
            );
        }
        
        return user;
    }

    public UserDTO withId(Long newId) {
        return new UserDTO(
            newId,
            this.username(),
            this.password(),
            this.email(),
            this.isAdmin(),
            this.potentiallyDangerous(),
            this.banned(),
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
            this.potentiallyDangerous(),
            this.banned(),
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
            this.potentiallyDangerous(),
            this.banned(),
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
            this.potentiallyDangerous(),
            this.banned(),
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
            this.potentiallyDangerous(),
            this.banned(),
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
            this.potentiallyDangerous(),
            this.banned(),
            this.imageUrl(),
            this.imageData(),
            newFollowers,
            this.following(),
            this.favoriteAlbumIds

        );
    }

    public UserDTO withIsAdmin(boolean newIsAdmin) {
        return new UserDTO(
            this.id(),
            this.username(),
            this.password(),
            this.email(),
            newIsAdmin,
            this.potentiallyDangerous(),
            this.banned(),
            this.imageUrl(),
            this.imageData(),
            this.followers(),
            this.following(),
            this.favoriteAlbumIds

        );
    }

    public UserDTO withPotentiallyDangerous(boolean newPotentiallyDangerous) {
        return new UserDTO(
            this.id(),
            this.username(),
            this.password(),
            this.email(),
            this.isAdmin(),
            newPotentiallyDangerous,
            this.banned(),
            this.imageUrl(),
            this.imageData(),
            this.followers(),
            this.following(),
            this.favoriteAlbumIds

        );
    }

    public UserDTO withBanned(boolean newBanned) {
        return new UserDTO(
            this.id(),
            this.username(),
            this.password(),
            this.email(),
            this.isAdmin(),
            this.potentiallyDangerous(),
            newBanned,
            this.imageUrl(),
            this.imageData(),
            this.followers(),
            this.following(),
            this.favoriteAlbumIds

        );
    }

    public UserDTO withPdfPath(String newPdfPath) {
        return new UserDTO(
            this.id(),
            this.username(),
            this.password(),
            this.email(),
            this.isAdmin(),
            this.potentiallyDangerous(),
            this.banned(),
            this.imageUrl(),
            this.imageData(),
            this.followers(),
            this.following(),
            this.favoriteAlbumIds,
            newPdfPath
        );
    }

    public UserDTO withoutImageData() {
        return new UserDTO(
            this.id(),
            this.username(),
            this.password(),
            this.email(),
            this.isAdmin(),
            this.potentiallyDangerous(),
            this.banned(),
            this.imageUrl(),
            null,
            this.followers(),
            this.following(),
            this.favoriteAlbumIds

        );
    }
}