package com.musicstore.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicstore.model.Album;
import com.musicstore.model.Review;
import com.musicstore.model.User;
import com.musicstore.model.Artist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DataLoader {

    @Autowired
    private AlbumService albumService;

    @Autowired
    private ArtistService artistService;

    @Autowired
    private UserService userService;


    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String DATA_DIR = System.getProperty("user.dir") + "/data";

    @PostConstruct
    public void loadData() {
        //loadAdminUser();
    }


    private void loadAdminUser() {
        if (userService.getAllUsers().isEmpty()) {
            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setPassword("admin");
            adminUser.setEmail("admin@echoreview.com");
            adminUser.setAdmin(true);
            try {
                userService.registerUser(adminUser);
                System.out.println("Usuario administrador creado exitosamente");
            } catch (Exception e) {
                System.err.println("Error al crear el usuario administrador: " + e.getMessage());
            }
        }
    }

    @Autowired
    private ReviewService reviewService;

    private void loadReviewsFromFile() {
        // No need to load reviews from file anymore as we're using database
        // Reviews will be handled by ReviewRepository and ReviewService
        System.out.println("Reviews will be managed through database operations");
    }

}