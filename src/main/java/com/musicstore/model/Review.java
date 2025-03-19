package com.musicstore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;


@Data
@Entity

public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "album_id")
    private Album album;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String username;
    private String userImageUrl;
    private String albumTitle;
    private String albumImageUrl;

    @NotBlank(message = "El contenido de la reseña es obligatorio")
    private String content;

    @Min(value = 1, message = "La calificación debe ser al menos 1")
    @Max(value = 5, message = "La calificación no puede ser mayor a 5")
    private int rating;

    // Getters and setters are handled by Lombok @Data

    public Long getAlbumId() {
        return album != null ? album.getId() : null;
    }

    public void setAlbumId(Long albumId) {
        if (this.album == null) {
            this.album = new Album();
        }
        this.album.setId(albumId);
    }

    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public void setUserId(Long userId) {
        if (this.user == null) {
            this.user = new User();
        }
        this.user.setId(userId);
    }
/*
    public String getAlbumTitle() {
        return albumTitle != null ? albumTitle : (album != null ? album.getTitle() : null);
    }

    public void setAlbumTitle(String albumTitle) {
        this.albumTitle = albumTitle;
    }

    public String getAlbumImageUrl() {
        return albumImageUrl != null ? albumImageUrl : (album != null ? album.getImageUrl() : null);
    }

    public void setAlbumImageUrl(String albumImageUrl) {
        this.albumImageUrl = albumImageUrl;
    }
    */
}