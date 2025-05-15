package com.echoreviews.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import java.sql.Blob;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Entity
public class Artist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    private String country;

    @JsonIgnore
    @ManyToMany(mappedBy = "artists")
    // @JsonIgnoreProperties({"reviews", "imageData", "audioData", "favoriteUsers"})
    private List<Album> albums = new ArrayList<>();

    private String imageUrl = "/images/default.jpg";

    @Lob
    @Column(name = "image_data", columnDefinition = "LONGBLOB")
    @JsonIgnore
    private byte[] imageData;

    // Constructor for string deserialization
    public Artist(String name) {
        this.name = name;
        this.country = "Unknown";
    }

    // Default constructor
    public Artist() {}

    public void addAlbum(Album album) {
        albums.add(album);
        album.getArtists().add(this);
    }

    public void removeAlbum(Album album) {
        albums.remove(album);
        album.getArtists().remove(this);
    }
}
