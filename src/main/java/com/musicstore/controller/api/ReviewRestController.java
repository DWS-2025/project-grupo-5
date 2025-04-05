package com.musicstore.controller.api;

import com.musicstore.model.Review;
import com.musicstore.service.AlbumService;
import com.musicstore.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.musicstore.dto.UserDTO;
import com.musicstore.dto.ArtistDTO;
import com.musicstore.dto.AlbumDTO;
import com.musicstore.dto.ReviewDTO;
import com.musicstore.mapper.UserMapper;
import com.musicstore.mapper.AlbumMapper;
import com.musicstore.mapper.ReviewMapper;
import com.musicstore.mapper.ArtistMapper;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewRestController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private AlbumService albumService;

    @Autowired
    private ReviewMapper reviewMapper;

    @GetMapping("/album/{albumId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByAlbum(@PathVariable Long albumId) {
        List<Review> reviews = reviewService.getReviewsByAlbumId(albumId);
        return ResponseEntity.ok(reviews.stream()
                .map(reviewMapper::toDTO)
                .toList());
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewDTO> getReviewById(@PathVariable Long reviewId) {
        try {
            return reviewService.getReviewById(null, reviewId)
                    .map(review -> ResponseEntity.ok(reviewMapper.toDTO(review)))
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

            Review review = reviewMapper.toEntity(reviewDTO);
            review.setAlbumId(albumId);
            Review savedReview = reviewService.addReview(albumId, review);

            // Update album's average rating
            albumService.getAlbumById(albumId).ifPresent(album -> {
                album.updateAverageRating(reviewService.getReviewsByAlbumId(albumId));
                albumService.saveAlbum(album);
            });

            return ResponseEntity.status(HttpStatus.CREATED).body(reviewMapper.toDTO(savedReview));
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
                            Review reviewToUpdate = reviewMapper.toEntity(reviewDTO);
                            existingReview.setContent(reviewToUpdate.getContent());
                            existingReview.setRating(reviewToUpdate.getRating());
                            Review updatedReview = reviewService.updateReview(albumId, existingReview);

                            // Update album's average rating
                            albumService.getAlbumById(albumId).ifPresent(album -> {
                                album.updateAverageRating(reviewService.getReviewsByAlbumId(albumId));
                                albumService.saveAlbum(album);
                            });

                            return ResponseEntity.ok(reviewMapper.toDTO(updatedReview));
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