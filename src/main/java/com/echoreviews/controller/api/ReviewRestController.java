package com.echoreviews.controller.api;

import com.echoreviews.model.Review;
import com.echoreviews.service.AlbumService;
import com.echoreviews.service.ReviewService;
import com.echoreviews.service.UserService;
import com.echoreviews.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.echoreviews.dto.ReviewDTO;
import com.echoreviews.dto.UserDTO;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/reviews")
public class ReviewRestController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private AlbumService albumService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<Page<Review>> getAllReviewsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Review> reviews = reviewService.getReviewsPaged(page, size);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/album/{albumId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByAlbum(@PathVariable Long albumId) {
        List<ReviewDTO> reviews = reviewService.getReviewsByAlbumId(albumId);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewDTO> getReviewById(@PathVariable Long reviewId) {
        try {
            return reviewService.getReviewById(null, reviewId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/album/{albumId}")
    public ResponseEntity<ReviewDTO> createReview(
            @PathVariable Long albumId,
            @RequestBody ReviewDTO reviewDTO,
            @RequestHeader("Authorization") String authHeader) {
        
        // Verify that the token exists and has the correct format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extract the token
        String token = authHeader.substring(7);

        try {
            // Get username from token
            String username = jwtUtil.extractUsername(token);
            
            // Get the user
            UserDTO user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Verify that the albumId exists
            albumService.getAlbumById(albumId)
                    .orElseThrow(() -> new RuntimeException("Album not found"));

            if (reviewDTO == null || reviewDTO.rating() < 1 || reviewDTO.rating() > 5 ||
                    reviewDTO.content() == null || reviewDTO.content().isBlank()) {
                return ResponseEntity.badRequest().build();
            }

            // Create a new review with user information
            ReviewDTO newReviewDTO = new ReviewDTO(
                null,
                albumId,
                user.id(),
                user.username(),
                user.imageUrl(),
                null, // albumTitle will be set in the service
                null, // albumImageUrl will be set in the service
                reviewDTO.content(),
                reviewDTO.rating()
            );

            Review savedReview = reviewService.addReview(albumId, newReviewDTO);
            ReviewDTO savedReviewDTO = ReviewDTO.fromReview(savedReview);

            return ResponseEntity.status(HttpStatus.CREATED).body(savedReviewDTO);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<ReviewDTO> createReviewGeneral(@RequestBody ReviewDTO reviewDTO) {
        try {
            if (reviewDTO == null || reviewDTO.rating() < 1 || reviewDTO.rating() > 5 ||
                    reviewDTO.content() == null || reviewDTO.content().isBlank() ||
                    reviewDTO.albumId() == null) {
                return ResponseEntity.badRequest().build();
            }

            Long albumId = reviewDTO.albumId();
            ReviewDTO savedReview = ReviewDTO.fromReview(reviewService.addReview(albumId, reviewDTO));

            return ResponseEntity.status(HttpStatus.CREATED).body(savedReview);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/album/{albumId}/review/{reviewId}")
    public ResponseEntity<ReviewDTO> updateReview(
            @PathVariable Long albumId,
            @PathVariable Long reviewId,
            @RequestBody ReviewDTO reviewDTO) {
        try {
            if (reviewDTO == null || reviewDTO.rating() < 1 || reviewDTO.rating() > 5 ||
                    reviewDTO.content() == null || reviewDTO.content().isBlank()) {
                return ResponseEntity.badRequest().build();
            }

            return (ResponseEntity<ReviewDTO>) reviewService.getReviewById(albumId, reviewId)
                    .map(existingReview -> {
                        try {
                            ReviewDTO updatedReview = reviewService.updateReview(albumId, reviewId, reviewDTO);
                            return ResponseEntity.ok(updatedReview);
                        } catch (RuntimeException e) {
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                        }
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewDTO> updateReviewById(
            @PathVariable Long reviewId,
            @RequestBody ReviewDTO reviewDTO,
            @RequestHeader("Authorization") String authHeader) {
        
        // Verify that the token exists and has the correct format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extract the token
        String token = authHeader.substring(7);

        try {
            // Get username from token
            String username = jwtUtil.extractUsername(token);
            
            // Get the user
            UserDTO user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (reviewDTO == null || reviewDTO.rating() < 1 || reviewDTO.rating() > 5 ||
                    reviewDTO.content() == null || reviewDTO.content().isBlank()) {
                return ResponseEntity.badRequest().build();
            }

            Optional<ReviewDTO> existingReviewOpt = reviewService.getReviewById(reviewId);
            if (existingReviewOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ReviewDTO existingReview = existingReviewOpt.get();
            
            // Verify that the user is the review owner or is admin
            if (!existingReview.username().equals(username) && !jwtUtil.isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            try {
                // Create the DTO with updated information but keeping user and album data
                ReviewDTO updatedReviewDTO = new ReviewDTO(
                    reviewId,
                    existingReview.albumId(),
                    existingReview.userId(),
                    existingReview.username(),
                    existingReview.userImageUrl(),
                    existingReview.albumTitle(),
                    existingReview.albumImageUrl(),
                    reviewDTO.content(),
                    reviewDTO.rating()
                );

                ReviewDTO updatedReview = reviewService.updateReview(updatedReviewDTO);
                return ResponseEntity.ok(updatedReview);
            } catch (RuntimeException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Object> deleteReview(
            @PathVariable Long reviewId,
            @RequestHeader("Authorization") String authHeader) {
        
        // Verify that the token exists and has the correct format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extract the token
        String token = authHeader.substring(7);

        try {
            // Get username from token
            String username = jwtUtil.extractUsername(token);
            
            // Get the user
            UserDTO user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return reviewService.getReviewById(reviewId)
                    .map(review -> {
                        // Verify that the user is the review owner or is admin
                        if (!review.username().equals(username) && !jwtUtil.isAdmin(token)) {
                            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                        }

                        try {
                            // Get albumId before deleting the review
                            Long albumId = review.albumId();
                            
                            reviewService.deleteReview(reviewId);

                            return ResponseEntity.noContent().build();
                        } catch (RuntimeException e) {
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                        }
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByUser(@PathVariable Long userId) {
        List<ReviewDTO> reviews = reviewService.getReviewsByUserId(userId);
        return ResponseEntity.ok(reviews);
    }
    
    @GetMapping("/details/{reviewId}")
    public ResponseEntity<?> getReviewDetails(@PathVariable Long reviewId, @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);
        
        Optional<UserDTO> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        UserDTO userDTO = userOpt.get();
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