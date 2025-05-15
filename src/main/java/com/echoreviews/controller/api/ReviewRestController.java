package com.echoreviews.controller.api;

import com.echoreviews.model.Review;
import com.echoreviews.service.AlbumService;
import com.echoreviews.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.echoreviews.dto.ReviewDTO;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewRestController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private AlbumService albumService;

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

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewDTO> updateReviewById(
            @PathVariable Long reviewId,
            @RequestBody ReviewDTO reviewDTO) {
        try {
            if (reviewDTO == null || reviewDTO.rating() < 1 || reviewDTO.rating() > 5 ||
                    reviewDTO.content() == null || reviewDTO.content().isBlank()) {
                return ResponseEntity.badRequest().build();
            }

            try {
                ReviewDTO updatedReview = reviewService.updateReviewById(reviewId, reviewDTO);

                // Update album's average rating
                Long albumId = updatedReview.albumId();
                albumService.getAlbumById(albumId).ifPresent(album -> {
                    album.updateAverageRating(reviewService.getReviewsByAlbumId(albumId));
                    albumService.saveAlbum(album);
                });

                return ResponseEntity.ok(updatedReview);
            } catch (RuntimeException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
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