package com.musicstore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import java.sql.Blob;

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

    @ManyToMany(mappedBy = "artists", cascade = CascadeType.REMOVE)
    private List<Album> albums = new ArrayList<>();

    public void addAlbum(Album album) {
        albums.add(album);
        album.getArtists().add(this);
    }

    public void removeAlbum(Album album) {
        albums.remove(album);
        album.getArtists().remove(this);
    }

    private String imageUrl = "/images/default.jpg";

    @Lob
    @Column(name= "audio_preview")
    private Blob imageFile;

}
