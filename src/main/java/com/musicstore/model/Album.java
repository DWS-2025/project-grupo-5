package com.musicstore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

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

    private String audioFile; // Nuevo campo

    private String description;

    private String tracklist; // Campo para la lista de canciones

    private Integer year; // Cambié la ubicación de year para mantener mejor orden

    private String spotify_url;

    private String applemusic_url;

    private String tidal_url;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}