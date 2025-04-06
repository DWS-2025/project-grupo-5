package com.musicstore.service;

import com.musicstore.model.Review;
import com.musicstore.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import com.musicstore.dto.ReviewDTO;
import com.musicstore.mapper.ReviewMapper;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewMapper reviewMapper;

    public List<Review> getReviewsByAlbumId(Long albumId) {
        return reviewRepository.findByAlbum_Id(albumId);
    }

    public Optional<Review> getReviewById(Long albumId, Long reviewId) {
        return reviewRepository.findById(reviewId);
    }

    public Review addReview(Long albumId, ReviewDTO reviewDTO) {
        Review review = reviewMapper.toEntity(reviewDTO);
        return reviewRepository.save(review);
    }

    public ReviewDTO updateReview(Long albumId, Long reviewId, ReviewDTO reviewDTO) {
        Review review = reviewMapper.toEntity(reviewDTO);
        review.setId(reviewId);
        return reviewMapper.toDTO(reviewRepository.save(review));
    }

    public void deleteReview(Long albumId, Long reviewId) {
        reviewRepository.deleteById(reviewId);
    }

    public List<Review> getReviewsByUserId(Long userId) {
        return reviewRepository.findByUser_Id(userId);
    }
}
