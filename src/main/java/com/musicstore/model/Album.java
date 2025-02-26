package com.musicstore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Data
public class Album {
    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Artist is required")
    private String artist;

    @NotBlank(message = "Genre is required")
    private String genre;

    private String imageUrl;

    @JsonIgnore
    private MultipartFile imageFile;

    @JsonIgnore
    private MultipartFile audioFile2;

    private String audioFile;

    private String description;

    private String tracklist;

    private Integer year;

    private String spotify_url;

    private String applemusic_url;

    private String tidal_url;

    private List<String> favoriteUsers = new ArrayList<>();

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