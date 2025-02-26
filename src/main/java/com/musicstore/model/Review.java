package com.musicstore.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Review {
    private Long id;
    private Long albumId; // Relación con el álbum
    private Long userId; // Relación con el usuario que realizó la reseña

    @NotBlank(message = "El contenido de la reseña es obligatorio")
    private String content; // Texto de la reseña

    @Min(value = 1, message = "La calificación debe ser al menos 1")
    @Max(value = 5, message = "La calificación no puede ser mayor a 5")
    private int rating; // Calificación del álbum (1-5)

    private String username; // Opcional: Mostrar el nombre del usuario

    // Transient fields for display purposes
    private String albumTitle;
    private String albumImageUrl;

    private List<String> Reviews = new ArrayList<>(); // IDs de álbumes favoritos
}