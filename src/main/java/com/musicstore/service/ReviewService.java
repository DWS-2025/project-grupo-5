package com.musicstore.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicstore.model.Review;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ReviewService {

    private final String FILE_PATH = System.getProperty("user.dir") + "/data/reviews.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<Long, List<Review>> reviewsByAlbum;

    public ReviewService() {
        loadReviewsFromFile();
    }

    private void loadReviewsFromFile() {
        File file = new File(FILE_PATH);
        if (file.exists() && file.length() > 0) {
            try {
                reviewsByAlbum = objectMapper.readValue(file, new TypeReference<Map<Long, List<Review>>>() {
                });
                System.out.println("Reseñas cargadas correctamente");
            } catch (IOException e) {
                System.err.println("Error cargando reseñas: " + e.getMessage());
                reviewsByAlbum = new HashMap<>();
            }
        } else {
            System.out.println("Archivo de reseñas no encontrado o vacío. Inicializando.");
            reviewsByAlbum = new HashMap<>();
            saveReviewsToFile();
        }
    }

    private synchronized void saveReviewsToFile() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), reviewsByAlbum);
        } catch (IOException e) {
            throw new RuntimeException("Error saving reviews to file", e);
        }
    }

    public List<Review> getReviewsByAlbumId(Long albumId) {
        return reviewsByAlbum.getOrDefault(albumId, new ArrayList<>());
    }

    public Optional<Review> getReviewById(Long albumId, Long reviewId) {
        return getReviewsByAlbumId(albumId).stream()
                .filter(review -> review.getId().equals(reviewId))
                .findFirst();
    }

    public Review addReview(Long albumId, Review review) {
        List<Review> albumReviews = reviewsByAlbum.getOrDefault(albumId, new ArrayList<>());
        review.setId(generateReviewId());
        albumReviews.add(review);
        reviewsByAlbum.put(albumId, albumReviews);
        saveReviewsToFile();
        return review;
    }

    public Review updateReview(Long albumId, Review updatedReview) {
        List<Review> albumReviews = getReviewsByAlbumId(albumId);
        int index = findReviewIndex(albumReviews, updatedReview.getId());

        if (index != -1) {
            albumReviews.set(index, updatedReview);
            saveReviewsToFile();
            return updatedReview;
        } else {
            throw new RuntimeException("Review not found");
        }
    }

    public void deleteReview(Long albumId, Long reviewId) {
        List<Review> albumReviews = getReviewsByAlbumId(albumId);
        boolean removed = albumReviews.removeIf(review -> review.getId().equals(reviewId));

        if (removed) {
            saveReviewsToFile();
        } else {
            throw new RuntimeException("Review not found");
        }
    }

    private int findReviewIndex(List<Review> reviews, Long reviewId) {
        for (int i = 0; i < reviews.size(); i++) {
            if (reviews.get(i).getId().equals(reviewId)) {
                return i;
            }
        }
        return -1;
    }

    public List<Review> getReviewsByUserId(Long userId) {
        if (userId == null || reviewsByAlbum == null) {
            return new ArrayList<>();
        }

        List<Review> userReviews = new ArrayList<>();
        for (List<Review> albumReviews : reviewsByAlbum.values()) {
            if (albumReviews != null) {
                userReviews.addAll(albumReviews.stream()
                        .filter(review -> review.getUserId() != null && review.getUserId().equals(userId))
                        .toList());
            }
        }

        return userReviews;
    }

    public void deleteReviewsByUser(String username) {
        // Iterate through all albums and their reviews
        for (List<Review> albumReviews : reviewsByAlbum.values()) {
            // Remove all reviews by the specified user
            albumReviews.removeIf(review -> review.getUsername().equals(username));
        }
        saveReviewsToFile();
    }

    private Long generateReviewId() {
        if (reviewsByAlbum.isEmpty()) {
            return 1L;
        }
        return reviewsByAlbum.values().stream()
                .flatMap(List::stream)
                .mapToLong(Review::getId)
                .max()
                .orElse(0L) + 1;
    }


}
