package com.musicstore.controller;


import com.musicstore.model.Review;
import com.musicstore.service.AlbumService;
import com.musicstore.service.ArtistService;
import com.musicstore.service.UserService;
import com.musicstore.service.ReviewService;
import com.musicstore.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;
import com.musicstore.dto.UserDTO;
import com.musicstore.dto.ArtistDTO;
import com.musicstore.dto.AlbumDTO;
import com.musicstore.dto.ReviewDTO;

import java.util.Optional;
import java.util.stream.Collectors;
import com.musicstore.mapper.UserMapper;
import com.musicstore.mapper.AlbumMapper;
import com.musicstore.mapper.ReviewMapper;
import com.musicstore.mapper.ArtistMapper;


@Controller
public class HomeController {
    @Autowired
    private AlbumService albumService;

    @Autowired
    private UserService userService;

    @Autowired
    private ArtistService artistService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserMapper userMapper;

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        model.addAttribute("albums", albumService.getAllAlbums());
        model.addAttribute("artist", artistService.getAllArtists());
        model.addAttribute("userService", userService);

        UserDTO userDTO = (UserDTO) session.getAttribute("user");
        if (userDTO == null) {
            userDTO = new UserDTO(null, null, null, null, false, null, null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
        model.addAttribute("user", userDTO);
        return "album/welcome";
    }

    @GetMapping("/{id}")
    public String viewAlbum(@PathVariable Long id, Model model, HttpSession session) {
        UserDTO userDTO = (UserDTO) session.getAttribute("user");
        if (userDTO == null) {
            userDTO = new UserDTO(null, null, null, null, false, null, null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
        model.addAttribute("user", userDTO);
        model.addAttribute("userService", userService);

        Optional<AlbumDTO> albumOptional = albumService.getAlbumById(id);
        if (albumOptional.isEmpty()) {
            model.addAttribute("error", "Album not found");
            return "error";
        }

        AlbumDTO album = albumOptional.get();
        model.addAttribute("album", album);

        // Get username and map users and albums
        List<String> usernames = userService.getUsernamesByAlbumId(album.id());
        model.addAttribute("favoriteUsernames", usernames);

        // Get reviews and map user IDs to usernames and profile images
        List<ReviewDTO> reviews = reviewService.getReviewsByAlbumId(id);
        reviews.forEach(review -> {
            // Los campos de usuario ya están en el ReviewDTO
            // No necesitamos obtener ni establecer información adicional del usuario
        });
        model.addAttribute("reviews", reviews);
        return "album/view";
    }
}

