package com.echoreviews.model;

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
    @JsonIgnore
    private String password;

    @Email(message = "Please provide a valid email address")
    @NotBlank(message = "Email is required")
    @JsonIgnore
    private String email;

    @Column(name = "is_admin")
    private boolean isAdmin = false;

    @Column(name = "potentially_dangerous")
    private boolean potentiallyDangerous = false;

    @Column(name = "banned")
    private boolean banned = false;

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

    private String pdfPath;

    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
    @JsonIgnoreProperties({"user", "album"})
    private List<Review> reviews = new ArrayList<>();
}