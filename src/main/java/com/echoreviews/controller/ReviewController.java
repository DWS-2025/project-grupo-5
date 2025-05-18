package com.echoreviews.controller;

import com.echoreviews.model.Album;
import com.echoreviews.model.User;
import com.echoreviews.service.AlbumService;
import com.echoreviews.service.ReviewService;
import com.echoreviews.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import com.echoreviews.dto.ReviewDTO;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import com.echoreviews.dto.UserDTO;
import com.echoreviews.dto.ArtistDTO;
import com.echoreviews.dto.AlbumDTO;
import com.echoreviews.dto.ReviewDTO;
import com.echoreviews.mapper.UserMapper;
import com.echoreviews.mapper.AlbumMapper;
import com.echoreviews.mapper.ReviewMapper;
import com.echoreviews.mapper.ArtistMapper;

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
            HttpSession session,
            Model model) {
        UserDTO userDTO = (UserDTO) session.getAttribute("user");
        if (userDTO != null) {
            if (rating < 1 || rating > 5 || content.isBlank()) {
                System.err.println("Invalid data. Review not saved.");
                return "redirect:/album/" + albumId;
            }
            if (content.length() > 255) {
                model.addAttribute("error", "Character limit exceeded");
                return "error";
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

            System.out.println("Saving user review: " + userDTO.username());
            reviewService.addReview(albumId, reviewDTO);
        }
        return "redirect:/album/" + albumId;
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
        UserDTO userDTO = (UserDTO) session.getAttribute("user");
        if (userDTO != null) {
            if (rating < 1 || rating > 5 || content.isBlank()) {
                return "redirect:/album/" + albumId;
            }
            if (content.length() > 255) {
                model.addAttribute("error", "Se ha superado el límite de caracteres");
                return "error";
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
            }
        }
        return "redirect:/album/" + albumId;
    }

    @PostMapping("/{albumId}/delete/{reviewId}")
    public String deleteReview(
            @PathVariable Long albumId,
            @PathVariable Long reviewId,
            HttpSession session
    ) {
        UserDTO userDTO = (UserDTO) session.getAttribute("user");
        if (userDTO != null) {
            ReviewDTO review = reviewService.getReviewById(albumId, reviewId).orElse(null);
            if (review != null && (review.username().equals(userDTO.username()) || userDTO.isAdmin())) {
                reviewService.deleteReview(albumId, reviewId);
            }
        }
        return "redirect:/album/" + albumId;
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

    @GetMapping("/details/{reviewId}")
    @ResponseBody
    public ResponseEntity<?> getReviewDetails(@PathVariable Long reviewId, HttpSession session) {
        UserDTO userDTO = (UserDTO) session.getAttribute("user");
        if (userDTO == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        Optional<ReviewDTO> reviewOpt = reviewService.getReviewByIdWithMarkdown(reviewId);
        
        if (reviewOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ReviewDTO review = reviewOpt.get();
        
        // Check if the user is authorized to edit this review
        if (!review.username().equals(userDTO.username()) && !userDTO.isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Not authorized to edit this review"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", review.content());
        response.put("rating", review.rating());
        
        return ResponseEntity.ok(response);
    }

}


