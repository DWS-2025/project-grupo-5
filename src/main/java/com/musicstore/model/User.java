package com.musicstore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.JoinTable;
import jakarta.persistence.JoinColumn;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
public class User {
    @Id
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

    @ManyToMany
    @JoinTable(
            name = "user_favorite_albums",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "album_id")
    )
    private List<Album> favoriteAlbums = new ArrayList<>();

    private String imageUrl = "/images/default.jpg";

    @Lob
    @Column(name= "audio_preview")
    private byte[] imageFile;

    private List<Long> followers = new ArrayList<>();
    private List<Long> following = new ArrayList<>();
}