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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

@Data
@Entity
public class Album {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    @JsonDeserialize(using = ArtistListDeserializer.class)
    @JsonAlias({"artist", "artists"})
    @ManyToMany
    @JoinTable(
            name = "album_artists",
            joinColumns = @JoinColumn(name = "album_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id")
    )
    private List<Artist> artists = new ArrayList<>();

    public Artist getArtist() {
        return artists != null && !artists.isEmpty() ? artists.get(0) : null;
    }

    public static class ArtistListDeserializer extends StdDeserializer<List<Artist>> {
        public ArtistListDeserializer() {
            super(List.class);
        }

        @Override
        public List<Artist> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            List<Artist> artists = new ArrayList<>();
            
            if (node.isArray()) {
                for (JsonNode artistNode : node) {
                    if (artistNode.isTextual()) {
                        artists.add(new Artist(artistNode.asText()));
                    }
                }
            } else if (node.isTextual()) {
                artists.add(new Artist(node.asText()));
            }
            
            return artists;
        }
    }

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

    @Lob
    @Column(name = "tracklist", columnDefinition = "TEXT")
    private String tracklist;


    @Column(name = "release_year")
    private Integer year;

    private String spotify_url;

    private String applemusic_url;

    private String tidal_url;

    @ManyToMany(mappedBy = "favoriteAlbums")
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