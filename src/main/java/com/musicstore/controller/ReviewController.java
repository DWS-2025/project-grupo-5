package com.musicstore.controller;

import com.musicstore.model.Album;
import com.musicstore.model.User;
import com.musicstore.service.AlbumService;
import com.musicstore.service.ReviewService;
import com.musicstore.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.musicstore.dto.ReviewDTO;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;
import com.musicstore.dto.UserDTO;
import com.musicstore.dto.ArtistDTO;
import com.musicstore.dto.AlbumDTO;
import com.musicstore.dto.ReviewDTO;
import com.musicstore.mapper.UserMapper;
import com.musicstore.mapper.AlbumMapper;
import com.musicstore.mapper.ReviewMapper;
import com.musicstore.mapper.ArtistMapper;

@Controller
@RequestMapping("/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;
    @Autowired
    private AlbumService albumService;
    @Autowired
    private UserService userService;

    @PostMapping("/{albumId}")
    public String addReview(
            @PathVariable Long albumId,
            @RequestParam String content,
            @RequestParam int rating,
            HttpSession session) {
        UserDTO userDTO = (UserDTO) session.getAttribute("user");
        if (userDTO != null) {
            if (rating < 1 || rating > 5 || content.isBlank()) {
                System.err.println("Datos inválidos. Reseña no guardada.");
                return "redirect:/" + albumId;
            }

            ReviewDTO reviewDTO = new ReviewDTO(
                    null,
                    albumId,
                    userDTO.id(),
                    userDTO.username(),
                    userDTO.imageUrl(),
                    null,
                    null,
                    content,
                    rating
            );

            System.out.println("Guardando reseña del usuario: " + userDTO.username());
            reviewService.addReview(albumId, reviewDTO);

            // Update album's average rating
            albumService.getAlbumById(albumId).ifPresent(albumDTO -> {
                albumDTO.updateAverageRating(reviewService.getReviewsByAlbumId(albumId));
                albumService.saveAlbumReview(albumDTO);
            });
        }
        return "redirect:/" + albumId;
    }

    @PostMapping("/{albumId}/edit/{reviewId}")
    public String editReview(
            @PathVariable Long albumId,
            @PathVariable Long reviewId,
            @RequestParam String content,
            @RequestParam int rating,
            HttpSession session,
            Model model
    ) {
        try {
            UserDTO userDTO = (UserDTO) session.getAttribute("user");
            if (userDTO == null) {
                return "redirect:/" + albumId;
            }

            if (rating < 1 || rating > 5 || content.isBlank()) {
                return "redirect:/" + albumId;
            }

            ReviewDTO existingReview = reviewService.getReviewById(albumId, reviewId).orElse(null);
            if (existingReview != null && existingReview.username().equals(userDTO.username())) {
                ReviewDTO reviewDTO = new ReviewDTO(
                        reviewId,
                        albumId,
                        userDTO.id(),
                        userDTO.username(),
                        userDTO.imageUrl(),
                        null,
                        null,
                        content,
                        rating
                );
                reviewService.updateReview(albumId, reviewId, reviewDTO);

                // Update album's average rating
                albumService.getAlbumById(albumId).ifPresent(albumDTO -> {
                    albumDTO.updateAverageRating(reviewService.getReviewsByAlbumId(albumId));
                    albumService.saveAlbum(albumDTO);
                });
            }
            return "redirect:/" + albumId;
        } catch (Exception e) {
            return "redirect:/" + albumId;
        }
    }

    @PostMapping("/{albumId}/delete/{reviewId}")
    public String deleteReview(
            @PathVariable Long albumId,
            @PathVariable Long reviewId,
            HttpSession session,
            Model model
    ) {
        try {
            UserDTO userDTO = (UserDTO) session.getAttribute("user");
            if (userDTO == null) {
                return "redirect:/" + albumId;
            }

            ReviewDTO review = reviewService.getReviewById(albumId, reviewId).orElse(null);
            if (review != null && (review.username().equals(userDTO.username()) || userDTO.isAdmin())) {
                reviewService.deleteReview(albumId, reviewId);

                // Update album's average rating
                albumService.getAlbumById(albumId).ifPresent(albumDTO -> {
                    albumDTO.updateAverageRating(reviewService.getReviewsByAlbumId(albumId));
                    albumService.saveAlbum(albumDTO);
                });
            }
            return "redirect:/" + albumId;
        } catch (Exception e) {
            return "redirect:/" + albumId;
        }
    }


    @GetMapping("/user/{username}")
    public String viewReviews(@PathVariable String username, Model model, HttpSession session) {
        UserDTO currentUser = (UserDTO) session.getAttribute("user");
        model.addAttribute("currentUser", currentUser);

        Optional<UserDTO> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "User not found");
            return "error";
        }

        UserDTO profileUser = userOpt.get();
        model.addAttribute("profileUser", profileUser);
        model.addAttribute("profileImageUrl", profileUser.imageUrl());

        ArrayList<Long> favoriteAlbums = new ArrayList<>(profileUser.favoriteAlbumIds());
        Collections.reverse(favoriteAlbums);
        favoriteAlbums = (ArrayList<Long>) favoriteAlbums.stream().limit(5).collect(Collectors.toList());
        model.addAttribute("favoriteAlbums", favoriteAlbums);

        List<ReviewDTO> userReviews = reviewService.getReviewsByUserId(profileUser.id());
        Collections.reverse(userReviews);
        model.addAttribute("userReviews", userReviews);

        return "reviews/user-review";
    }

}


