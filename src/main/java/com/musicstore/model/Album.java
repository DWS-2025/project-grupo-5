package com.musicstore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;
import java.util.ArrayList;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.CascadeType;
import java.sql.Blob;

@Data
@Entity
public class Album {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    @ManyToMany
    @JoinTable(
            name = "album_artists",
            joinColumns = @JoinColumn(name = "album_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id")
    )
    private List<Artist> artists = new ArrayList<>();

    // Add OneToMany relationship with Review
    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL)
    private List<Review> reviews = new ArrayList<>();

    @NotBlank(message = "Genre is required")
    private String genre;

    private String imageUrl;

    @Lob
    @Column(name = "image_file")
    private Blob imageFile;

    @Lob
    @Column(name = "audio_preview")
    private Blob audioFile2;

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

    // Update the average rating based on provided reviews
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