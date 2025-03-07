package com.musicstore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Data
public class Artist {
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Country is required")
    private String country;

    private List<Long> AlbumIds = new ArrayList<>();

    private String imageUrl = "/images/default.jpg";

    @JsonIgnore
    private MultipartFile imageFile;

}
