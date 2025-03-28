package com.musicstore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import java.sql.Blob;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@Entity
public class Artist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;



    @NotBlank(message = "Country is required")
    private String country;

    @ManyToMany(mappedBy = "artists")
    private List<Album> albums = new ArrayList<>();

    private String imageUrl = "/images/default.jpg";

    @Lob
    @Column(name = "image_file")
    private Blob imageFile;

    @Transient
    @JsonIgnore
    private MultipartFile uploadFile;

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
    /*
    @Override
    public String toString() {
        return name;
    }

     */

}
