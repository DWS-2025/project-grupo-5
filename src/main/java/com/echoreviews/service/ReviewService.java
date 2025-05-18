package com.echoreviews.service;

import com.echoreviews.dto.ReviewDTO;
import com.echoreviews.mapper.ReviewMapper;
import com.echoreviews.model.Album;
import com.echoreviews.model.Review;
import com.echoreviews.model.User;
import com.echoreviews.repository.AlbumRepository;
import com.echoreviews.repository.ReviewRepository;
import com.echoreviews.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import jakarta.transaction.Transactional;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewMapper reviewMapper;

    public List<ReviewDTO> getReviewsByAlbumId(Long albumId) {
        List<Review> reviews = reviewRepository.findByAlbum_Id(albumId);
        return reviews.stream()
                .map(reviewMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Page<Review> getReviewsPaged(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewRepository.findAll(pageable);
    }

    public Optional<ReviewDTO> getReviewById(Long albumId, Long reviewId) {
        Optional<Review> reviewOpt = reviewRepository.findById(reviewId);
        
        // If albumId is null, we don't filter by album (for REST API)
        if (albumId == null) {
            return reviewOpt.map(reviewMapper::toDTO);
        }
        
        // If albumId is not null, we verify that the review belongs to the album
        return reviewOpt
                .filter(review -> review.getAlbum().getId().equals(albumId))
                .map(reviewMapper::toDTO);
    }
    
    public Optional<ReviewDTO> getReviewById(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .map(reviewMapper::toDTO);
    }
    
    public List<ReviewDTO> getAllReviews() {
        return reviewRepository.findAll().stream()
                .map(reviewMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<ReviewDTO> getAllReviewsWithMarkdown() {
        return reviewRepository.findAll().stream()
                .map(reviewMapper::toDTOWithMarkdown)
                .collect(Collectors.toList());
    }

    public Review addReview(Long albumId, ReviewDTO reviewDTO) {
        if (reviewDTO == null) {
            throw new IllegalArgumentException("ReviewDTO cannot be null");
        }

        if (!albumId.equals(reviewDTO.albumId())) {
            throw new RuntimeException("Album ID mismatch between path and review data");
        }

        // Raw Markdown from reviewDTO is now used directly
        Review review = reviewMapper.toEntity(reviewDTO);
        Review savedReview = reviewRepository.save(review);

        // Update album's average rating
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("Album not found"));
        
        List<Review> albumReviews = reviewRepository.findByAlbum_Id(albumId);
        album.updateAverageRating(albumReviews);
        albumRepository.save(album);

        return savedReview;
    }

    public ReviewDTO updateReview(Long albumId, Long reviewId, ReviewDTO reviewDTO) {
        Review existing = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (!existing.getAlbum().getId().equals(albumId)) {
            throw new RuntimeException("Review does not belong to this album");
        }

        // Raw Markdown from reviewDTO is now used directly
        Review updated = reviewMapper.toEntity(reviewDTO);
        updated.setId(reviewId);
        updated.setAlbum(existing.getAlbum()); // Ensure the album doesn't change
        Review savedReview = reviewRepository.save(updated);

        // Update album's average rating
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("Album not found"));
        
        List<Review> albumReviews = reviewRepository.findByAlbum_Id(albumId);
        album.updateAverageRating(albumReviews);
        albumRepository.save(album);

        return reviewMapper.toDTO(savedReview);
    }
    
    public ReviewDTO updateReviewById(Long reviewId, ReviewDTO reviewDTO) {
        Review existing = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        
        // Raw Markdown from reviewDTO is now used directly
        Review updated = reviewMapper.toEntity(reviewDTO);
        updated.setId(reviewId);
        updated.setAlbum(existing.getAlbum()); // Ensure the album doesn't change
        Review savedReview = reviewRepository.save(updated);
        return reviewMapper.toDTO(savedReview);
    }
    
    @Transactional
    public ReviewDTO updateReview(ReviewDTO reviewDTO) {
        if (reviewDTO.id() == null) {
            throw new RuntimeException("Review ID cannot be null");
        }
        
        Review existing = reviewRepository.findById(reviewDTO.id())
                .orElseThrow(() -> new RuntimeException("Review not found"));
        
        // Raw Markdown from reviewDTO is now used directly
        Review updated = reviewMapper.toEntity(reviewDTO);
        // Ensure ID and Album are preserved if not part of DTO or should not be changed by this map
        updated.setId(existing.getId());
        updated.setAlbum(existing.getAlbum());
        updated.setUser(existing.getUser()); // Also preserve user

        Review savedReview = reviewRepository.save(updated);
        return reviewMapper.toDTO(savedReview);
    }

    public void deleteReview(Long albumId, Long reviewId) {
        Review existing = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (albumId != null && !existing.getAlbum().getId().equals(albumId)) {
            throw new RuntimeException("Review does not belong to this album");
        }

        reviewRepository.deleteById(reviewId);

        // Update album's average rating
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("Album not found"));
        
        List<Review> albumReviews = reviewRepository.findByAlbum_Id(albumId);
        album.updateAverageRating(albumReviews);
        albumRepository.save(album);
    }
    
    public void deleteReview(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new RuntimeException("Review not found");
        }
        reviewRepository.deleteById(reviewId);
    }

    public List<ReviewDTO> getReviewsByUserId(Long userId) {
        List<Review> reviews = reviewRepository.findByUser_Id(userId);
        return reviews.stream()
                .map(reviewMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<ReviewDTO> getReviewByIdWithMarkdown(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .map(reviewMapper::toDTOWithMarkdown);
    }
}
