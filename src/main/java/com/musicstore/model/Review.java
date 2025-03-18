package com.musicstore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity

public class Review {
    @Id
    private Long id;
    private Long albumId;
    private Long userId;

    @NotBlank(message = "El contenido de la reseña es obligatorio")
    private String content;

    @Min(value = 1, message = "La calificación debe ser al menos 1")
    @Max(value = 5, message = "La calificación no puede ser mayor a 5")
    private int rating;

    private String username;
    private String userImageUrl;

    public void setUserImageUrl(String imageUrl) {
        this.userImageUrl = imageUrl;
    }

    // Transient fields for display purposes
    private String albumTitle;
    private String albumImageUrl;
    @ManyToMany
    private List<String> Reviews = new ArrayList<>();
}