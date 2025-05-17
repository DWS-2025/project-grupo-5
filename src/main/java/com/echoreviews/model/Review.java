package com.echoreviews.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Entity
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "album_id", nullable = false)
    @JsonIgnoreProperties({"reviews", "artists", "imageData", "audioData", "favoriteUsers"})
    private Album album;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"favoriteAlbums", "password", "email", "imageData", "followers", "following"})
    private User user;

    private String username;
    private String userImageUrl;
    private String albumTitle;
    private String albumImageUrl;

    @NotBlank(message = "Review content is required")
    @Size(max = 2000, message = "Review cannot exceed 2000 characters")
    private String content;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot be greater than 5")
    private int rating;

    // Getters and setters for relationships
    public Long getAlbumId() {
        return album != null ? album.getId() : null;
    }

    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public void setAlbumId(Long albumId) {
        if (this.album == null) {
            this.album = new Album();
        }
        this.album.setId(albumId);
    }

    public void setUserId(Long userId) {
        if (this.user == null) {
            this.user = new User();
        }
        this.user.setId(userId);
    }

    // Getters and setters for denormalized fields
    public String getAlbumTitle() {
        return albumTitle;
    }

    public void setAlbumTitle(String albumTitle) {
        this.albumTitle = albumTitle;
    }

    public String getAlbumImageUrl() {
        return albumImageUrl;
    }

    public void setAlbumImageUrl(String albumImageUrl) {
        this.albumImageUrl = albumImageUrl;
    }
}