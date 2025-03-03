package com.musicstore.controller;

import com.musicstore.model.Album;
import com.musicstore.model.Review;
import com.musicstore.model.User;
import com.musicstore.service.AlbumService;
import com.musicstore.service.ReviewService;
import com.musicstore.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        User user = (User) session.getAttribute("user");
        if (user != null) {
            if (rating < 1 || rating > 5 || content.isBlank()) {
                System.err.println("Datos inválidos. Reseña no guardada.");
                return "redirect:/" + albumId;
            }

            Review review = new Review();
            review.setContent(content);
            review.setRating(rating);
            review.setUsername(user.getUsername());
            review.setUserId(user.getId());
            review.setAlbumId(albumId);

            System.out.println("Guardando reseña del usuario: " + user.getUsername());
            reviewService.addReview(albumId, review);

            // Update album's average rating
            albumService.getAlbumById(albumId).ifPresent(album -> {
                album.updateAverageRating(reviewService.getReviewsByAlbumId(albumId));
                albumService.saveAlbum(album);
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
            HttpSession session
    ) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            if (rating < 1 || rating > 5 || content.isBlank()) {
                return "redirect:/" + albumId;
            }

            Review existingReview = reviewService.getReviewById(albumId, reviewId).orElse(null);
            if (existingReview != null && existingReview.getUsername().equals(user.getUsername())) {
                existingReview.setContent(content);
                existingReview.setRating(rating);
                reviewService.updateReview(albumId, existingReview);

                // Update album's average rating
                albumService.getAlbumById(albumId).ifPresent(album -> {
                    album.updateAverageRating(reviewService.getReviewsByAlbumId(albumId));
                    albumService.saveAlbum(album);
                });
            }
        }
        return "redirect:/" + albumId;
    }

    @PostMapping("/{albumId}/delete/{reviewId}")
    public String deleteReview(
            @PathVariable Long albumId,
            @PathVariable Long reviewId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            Review review = reviewService.getReviewById(albumId, reviewId).orElse(null);
            if (review != null && (review.getUsername().equals(user.getUsername()) || user.isAdmin())) {
                reviewService.deleteReview(albumId, reviewId);

                // Update album's average rating
                albumService.getAlbumById(albumId).ifPresent(album -> {
                    album.updateAverageRating(reviewService.getReviewsByAlbumId(albumId));
                    albumService.saveAlbum(album);
                });
            }
        }
        return "redirect:/" + albumId;
    }


    @GetMapping("/user/{username}")
    public String viewReviews(@PathVariable String username, Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        model.addAttribute("currentUser", currentUser);

        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "User not found");
            return "error";
        }

        User profileUser = userOpt.get();
        model.addAttribute("profileUser", profileUser);

        String profileImageUrl = profileUser.getImageUrl();
        model.addAttribute("profileImageUrl", profileImageUrl);

        List<Album> favoriteAlbums = profileUser.getFavoriteAlbumIds().stream()
                .map(albumId -> albumService.getAlbumById(albumId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        Collections.reverse(favoriteAlbums);
        favoriteAlbums = favoriteAlbums.stream().limit(5).collect(Collectors.toList());
        model.addAttribute("favoriteAlbums", favoriteAlbums);

        List<Review> userReviews = reviewService.getReviewsByUserId(profileUser.getId());
        userReviews.forEach(review -> {
            albumService.getAlbumById(review.getAlbumId()).ifPresent(album -> {
                review.setAlbumTitle(album.getTitle());
                review.setAlbumImageUrl(album.getImageUrl());
            });
        });

        Collections.reverse(userReviews);
        userReviews = userReviews.stream().limit(5).collect(Collectors.toList());
        model.addAttribute("userReviews", userReviews);

        return "reviews/user-review";
    }


}


