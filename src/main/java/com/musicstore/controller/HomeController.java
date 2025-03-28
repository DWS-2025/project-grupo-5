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
import java.util.List;
import java.util.stream.Collectors;


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

    @GetMapping("/")
    public String home(Model model, HttpSession session) {

        model.addAttribute("albums", albumService.getAllAlbums());
        model.addAttribute("artist", artistService.getAllArtists());
        User user = (User) session.getAttribute("user");

        if (user != null) {
            model.addAttribute("user", user);
        } else {
            User anonymousUser = new User();
            model.addAttribute("user", anonymousUser);
        }
        return "album/welcome";
    }

    @GetMapping("/{id}")
    public String viewAlbum(@PathVariable Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("user", user);
        } else {
            User anonymousUser = new User();
            model.addAttribute("user", anonymousUser);
        }

        if (albumService.getAlbumById(id).isEmpty()) {
            model.addAttribute("error", "Album not found");
            return "error";
        }

        albumService.getAlbumById(id).ifPresent(album -> {
            model.addAttribute("album", album);
            // Get username and map users and albums
            List<String> usernames = userService.getUsernamesByAlbumId(album.getId());
            model.addAttribute("favoriteUsernames", usernames);

            // Get reviews and map user IDs to usernames and profile images
            List<Review> reviews = reviewService.getReviewsByAlbumId(id);
            reviews.forEach(review -> {
                if (review.getUser() != null) {
                    review.setUsername(review.getUser().getUsername());
                    review.setUserImageUrl(review.getUser().getImageUrl());
                }
            });
            model.addAttribute("reviews", reviews);
        });
        return "album/view";
    }
}

