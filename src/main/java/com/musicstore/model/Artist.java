package com.musicstore.model;

import jakarta.persistence.*;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


import java.util.ArrayList;
import java.util.List;

@Data
@Entity
public class Artist {
    @Id
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Country is required")
    private String country;

    @ManyToOne
    private List<Long> AlbumIds = new ArrayList<>();

    private String imageUrl = "/images/default.jpg";

    @Lob
    @Column(name= "audio_preview")
    private byte[] imageFile;

}
