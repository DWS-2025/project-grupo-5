package com.musicstore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;
import java.util.ArrayList;

@Data
@Entity
public class Album {
    @Id
    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Artist is required")
    private String artist;

    @NotBlank(message = "Genre is required")
    private String genre;

    private String imageUrl;

    @Lob
    @Column(name= "audio_preview")
    private byte[] imageFile;

    @Lob
    @Column(name= "audio_preview")
    private byte[] audioFile2;

    private String audioFile;

    private String description;

    private String tracklist;

    private Integer year;

    private String spotify_url;

    private String applemusic_url;

    private String tidal_url;

    @ManyToMany(mappedBy = "favoriteAlbums")
    private List<User> favoriteUsers = new ArrayList<>();

    private Double averageRating = 0.0;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void updateAverageRating(List<Review> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            this.averageRating = 0.0;
            return;
        }
        double sum = reviews.stream()
                .mapToInt(Review::getRating)
                .sum();
        this.averageRating = sum / reviews.size();
    }
}