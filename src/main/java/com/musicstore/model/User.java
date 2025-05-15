package com.musicstore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#$%^&*(),.?\":{}|<>])(?=\\S+$).{8,25}$", 
            message = "Password must be between 8 and 25 characters long and contain at least one number, one uppercase letter, and one special character")
    @JsonIgnore
    private String password;

    @Email(message = "Please provide a valid email address")
    @NotBlank(message = "Email is required")
    @JsonIgnore
    private String email;

    private boolean isAdmin = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_favorite_albums",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "album_id")
    )
    @JsonIgnoreProperties({"favoriteUsers"})
    private List<Album> favoriteAlbums = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_followers",
            joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "follower_id")
    private List<Long> followers = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_following",
            joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "following_id")
    private List<Long> following = new ArrayList<>();

    private String imageUrl = "/images/default.jpg";

    @Lob
    @Column(name = "image_data", columnDefinition = "LONGBLOB")
    @JsonIgnoreProperties
    private byte[] imageData;

    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
    @JsonIgnoreProperties({"user", "album"})
    private List<Review> reviews = new ArrayList<>();
}