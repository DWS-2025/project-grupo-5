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
        
        // Verificar que el token existe y tiene el formato correcto
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extraer el token
        String token = authHeader.substring(7);

        try {
            // Obtener el username del token
            String username = jwtUtil.extractUsername(token);
            
            // Obtener el usuario
            UserDTO user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Verificar que el albumId existe
            albumService.getAlbumById(albumId)
                    .orElseThrow(() -> new RuntimeException("Album not found"));

            if (reviewDTO == null || reviewDTO.rating() < 1 || reviewDTO.rating() > 5 ||
                    reviewDTO.content() == null || reviewDTO.content().isBlank()) {
                return ResponseEntity.badRequest().build();
            }

            // Crear una nueva review con la información del usuario
            ReviewDTO newReviewDTO = new ReviewDTO(
                null,
                albumId,
                user.id(),
                user.username(),
                user.imageUrl(),
                null, // albumTitle se establecerá en el servicio
                null, // albumImageUrl se establecerá en el servicio
                reviewDTO.content(),
                reviewDTO.rating()
            );

            Review savedReview = reviewService.addReview(albumId, newReviewDTO);
            ReviewDTO savedReviewDTO = ReviewDTO.fromReview(savedReview);

            // Update album's average rating
            albumService.getAlbumById(albumId).ifPresent(album -> {
                album.updateAverageRating(reviewService.getReviewsByAlbumId(albumId));
                albumService.saveAlbum(album);
            });

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
            @RequestBody ReviewDTO reviewDTO,
            @RequestHeader("Authorization") String authHeader) {
        
        // Verificar que el token existe y tiene el formato correcto
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extraer el token
        String token = authHeader.substring(7);

        try {
            // Obtener el username del token
            String username = jwtUtil.extractUsername(token);
            
            // Obtener el usuario
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
            
            // Verificar que el usuario es el dueño de la review o es admin
            if (!existingReview.username().equals(username) && !jwtUtil.isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            try {
                // Crear el DTO con la información actualizada pero manteniendo los datos del usuario y álbum
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
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Object> deleteReview(
            @PathVariable Long reviewId,
            @RequestHeader("Authorization") String authHeader) {
        
        // Verificar que el token existe y tiene el formato correcto
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extraer el token
        String token = authHeader.substring(7);

        try {
            // Obtener el username del token
            String username = jwtUtil.extractUsername(token);
            
            // Obtener el usuario
            UserDTO user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return reviewService.getReviewById(reviewId)
                    .map(review -> {
                        // Verificar que el usuario es el dueño de la review o es admin
                        if (!review.username().equals(username) && !jwtUtil.isAdmin(token)) {
                            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                        }

                        try {
                            // Obtener el albumId antes de eliminar la review
                            Long albumId = review.albumId();
                            
                            reviewService.deleteReview(reviewId);

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
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByUser(@PathVariable Long userId) {
        try {
            List<ReviewDTO> reviews = reviewService.getReviewsByUserId(userId);
            return ResponseEntity.ok(reviews);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}