package com.musicstore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Data
public class User {
    private Long id;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    @Email(message = "Please provide a valid email address")
    @NotBlank(message = "Email is required")
    private String email;

    private boolean isAnonymous = false;
    
    private boolean isAdmin = false;

    private List<Long> favoriteAlbumIds = new ArrayList<>();

    private String imageUrl = "/images/default.jpg";

    @JsonIgnore
    private MultipartFile imageFile;

    private List<Long> followers = new ArrayList<>();
    private List<Long> following = new ArrayList<>();
}