package com.musicstore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import jakarta.persistence.Transient;

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

    @Transient
    @JsonIgnore
    private MultipartFile imageFile;

    private String audioFile; // Nuevo campo

    private String description;

    private Integer year; // Cambié la ubicación de year para mantener mejor orden

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}