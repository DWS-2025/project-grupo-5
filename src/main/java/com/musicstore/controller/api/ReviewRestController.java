package com.musicstore.controller.api;

import com.musicstore.model.Review;
import com.musicstore.service.AlbumService;
import com.musicstore.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.musicstore.dto.ReviewDTO;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewRestController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private AlbumService albumService;

    @GetMapping("/album/{albumId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByAlbum(@PathVariable Long albumId) {
        List<Review> reviews = reviewService.getReviewsByAlbumId(albumId);
        List<ReviewDTO> reviewDTOs = reviews.stream()
                .map(ReviewDTO::fromReview)
                .toList();
        return ResponseEntity.ok(reviewDTOs);
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<Review> getReviewById(@PathVariable Long reviewId) {
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
            @RequestBody ReviewDTO reviewDTO) {
        try {
            if (reviewDTO == null || reviewDTO.rating() < 1 || reviewDTO.rating() > 5 || 
                reviewDTO.content() == null || reviewDTO.content().isBlank()) {
                return ResponseEntity.badRequest().build();
            }

            ReviewDTO savedReview = ReviewDTO.fromReview(reviewService.addReview(albumId, reviewDTO));

            // Update album's average rating
            albumService.getAlbumById(albumId).ifPresent(album -> {
                album.updateAverageRating(reviewService.getReviewsByAlbumId(albumId));
                albumService.saveAlbum(album);
            });

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

                            // Update album's average rating
                            albumService.getAlbumById(albumId).ifPresent(album -> {
                                album.updateAverageRating(reviewService.getReviewsByAlbumId(albumId));
                                albumService.saveAlbum(album);
                            });

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

    @DeleteMapping("/album/{albumId}/review/{reviewId}")
    public ResponseEntity<Object> deleteReview(
            @PathVariable Long albumId,
            @PathVariable Long reviewId) {
        try {
            return reviewService.getReviewById(albumId, reviewId)
                    .map(review -> {
                        try {
                            reviewService.deleteReview(albumId, reviewId);

                            // Update album's average rating
                            albumService.getAlbumById(albumId).ifPresent(album -> {
                                album.updateAverageRating(reviewService.getReviewsByAlbumId(albumId));
                                albumService.saveAlbum(album);
                            });

                            return ResponseEntity.noContent().build();
                        } catch (RuntimeException e) {
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                        }
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}