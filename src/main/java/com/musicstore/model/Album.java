package com.musicstore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

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

    private String audioFile; // Nuevo campo

    private Double price;

    private String description;

    private Integer year; // Cambié la ubicación de year para mantener mejor orden

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}