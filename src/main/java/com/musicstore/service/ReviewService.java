package com.musicstore.service;

import com.musicstore.model.Review;
import com.musicstore.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import com.musicstore.dto.ReviewDTO;
import com.musicstore.mapper.ReviewMapper;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

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
        return reviewRepository.findById(reviewId)
                .filter(review -> review.getAlbum().getId().equals(albumId))
                .map(reviewMapper::toDTO);
    }

    public Review addReview(Long albumId, ReviewDTO reviewDTO) {
        if (reviewDTO == null) {
            throw new IllegalArgumentException("ReviewDTO cannot be null");
        }

        if (!albumId.equals(reviewDTO.albumId())) {
            throw new RuntimeException("Album ID mismatch between path and review data");
        }

        // Aquí podrías validar también si hay contenido mínimo o puntuación válida, si procede

        Review review = reviewMapper.toEntity(reviewDTO);
        return reviewRepository.save(review);
    }


    public ReviewDTO updateReview(Long albumId, Long reviewId, ReviewDTO reviewDTO) {
        Review existing = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (!existing.getAlbum().getId().equals(albumId)) {
            throw new RuntimeException("Review does not belong to this album");
        }

        Review updated = reviewMapper.toEntity(reviewDTO);
        updated.setId(reviewId);
        updated.setAlbum(existing.getAlbum()); // Aseguramos que no cambie el álbum
        Review savedReview = reviewRepository.save(updated);
        return reviewMapper.toDTO(savedReview);
    }

    public void deleteReview(Long albumId, Long reviewId) {
        Review existing = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (!existing.getAlbum().getId().equals(albumId)) {
            throw new RuntimeException("Review does not belong to this album");
        }

        reviewRepository.deleteById(reviewId);
    }

    public List<ReviewDTO> getReviewsByUserId(Long userId) {
        List<Review> reviews = reviewRepository.findByUser_Id(userId);
        return reviews.stream()
                .map(reviewMapper::toDTO)
                .collect(Collectors.toList());
    }
}
