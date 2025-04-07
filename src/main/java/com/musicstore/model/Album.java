package com.musicstore.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.CascadeType;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonBackReference;

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
    @JsonIgnoreProperties({"albums", "imageData"})
    private List<Artist> artists = new ArrayList<>();

    public Artist getArtist() {
        return artists != null && !artists.isEmpty() ? artists.get(0) : null;
    }

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"album", "user"})
    private List<Review> reviews = new ArrayList<>();

    @NotBlank(message = "Genre is required")
    private String genre;

    private String imageUrl;

    @Lob
    @Column(name = "image_data")
    @JsonIgnoreProperties
    private byte[] imageData;

    @Lob
    @Column(name = "audio_data")
    @JsonIgnoreProperties
    private byte[] audioData;

    private String audioFile;

    @Lob
    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;


    @Column(name = "tracklist", columnDefinition = "TEXT")
    private String tracklist;


    @Column(name = "release_year")
    private Integer year;

    private String spotify_url;

    private String applemusic_url;

    private String tidal_url;

    @ManyToMany(mappedBy = "favoriteAlbums")
    @JsonIgnoreProperties({"favoriteAlbums", "password", "email", "imageData", "followers", "following"})
    private List<User> favoriteUsers = new ArrayList<>();

    private Double averageRating = 0.0;

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

    // Helper methods for managing artist relationships
    public void addArtist(Artist artist) {
        artists.add(artist);
        artist.getAlbums().add(this);
    }

    public void removeArtist(Artist artist) {
        artists.remove(artist);
        artist.getAlbums().remove(this);
    }
}