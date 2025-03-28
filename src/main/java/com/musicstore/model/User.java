package com.musicstore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import java.sql.Blob;

@Data
@Entity
@Table(name = "users")
public class User {
    // Constructor for string deserialization
    public User(String id) {
        this.id = Long.parseLong(id);
    }

    // Default constructor
    public User() {}
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    @Email(message = "Please provide a valid email address")
    @NotBlank(message = "Email is required")
    private String email;

    private boolean isAdmin = false;

    @ManyToMany
    @JoinTable(
            name = "user_favorite_albums",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "album_id")
    )
    private List<Album> favoriteAlbums = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "user_followers",
            joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "follower_id")
    private List<Long> followers = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "user_following",
            joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "following_id")
    private List<Long> following = new ArrayList<>();

    private String imageUrl = "/images/default.jpg";

    @Lob
    @Column(name = "image_data")
    private byte[] imageData;


}