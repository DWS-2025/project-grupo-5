package com.echoreviews.controller;


import com.echoreviews.model.Review;
import com.echoreviews.service.AlbumService;
import com.echoreviews.service.ArtistService;
import com.echoreviews.service.UserService;
import com.echoreviews.service.ReviewService;
import com.echoreviews.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;
import com.echoreviews.dto.UserDTO;
import com.echoreviews.dto.ArtistDTO;
import com.echoreviews.dto.AlbumDTO;
import com.echoreviews.dto.ReviewDTO;

import java.util.Optional;
import java.util.stream.Collectors;
import com.echoreviews.mapper.UserMapper;
import com.echoreviews.mapper.AlbumMapper;
import com.echoreviews.mapper.ReviewMapper;
import com.echoreviews.mapper.ArtistMapper;


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
            userDTO = new UserDTO(null, null, null, null, false, false, false, null, null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
        model.addAttribute("user", userDTO);
        return "album/welcome";
    }

    @GetMapping("/album/{id}")
    public String viewAlbum(@PathVariable Long id, Model model, HttpSession session) {
        UserDTO userDTO = (UserDTO) session.getAttribute("user");
        if (userDTO == null) {
            userDTO = new UserDTO(null, null, null, null, false, false, false, null, null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
        model.addAttribute("user", userDTO);
        model.addAttribute("userService", userService);

        Optional<AlbumDTO> albumOptional = albumService.getAlbumById(id);
        if (albumOptional.isEmpty()) {
            model.addAttribute("error", "Album not found");
            return "error";
        }

        // Get reviews and update average before showing the album
        List<ReviewDTO> reviews = reviewService.getReviewsByAlbumId(id);
        AlbumDTO album = albumOptional.get().updateAverageRating(reviews);
        model.addAttribute("album", album);

        // Get username and map users and albums
        List<String> usernames = userService.getUsernamesByAlbumId(album.id());
        model.addAttribute("favoriteUsernames", usernames);

        // Get reviews y map user IDs to usernames and profile images
        model.addAttribute("reviews", reviews);
        return "album/view";
    }
}

