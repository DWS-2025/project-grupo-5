package com.musicstore.controller;

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
import java.util.List;

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
            HttpSession session
    ) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            if (rating < 1 || rating > 5 || content.isBlank()) {
                System.err.println("Datos inválidos. Reseña no guardada.");
                return "redirect:/" + albumId + "?error=invalidReview";
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
                return "redirect:/" + albumId + "?error=invalidReview";
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

    @GetMapping("/{albumId}")
    public String getReviewsByAlbumId(@PathVariable Long albumId, Model model) {
        model.addAttribute("reviews", reviewService.getReviewsByAlbumId(albumId));
        return "reviews/list";
    }

    @GetMapping("/my-reviews")
    public String getUserReviews(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<Review> userReviews = reviewService.getReviewsByUserId(user.getId());
        userReviews.forEach(review -> {
            albumService.getAlbumById(review.getAlbumId()).ifPresent(album -> {
                review.setAlbumTitle(album.getTitle());
                review.setAlbumImageUrl(album.getImageUrl());
            });
        });
        model.addAttribute("userReviews", userReviews);
        return "reviews/my-reviews";
    }
}
