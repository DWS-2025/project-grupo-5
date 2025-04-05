package com.musicstore.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicstore.model.Review;
import com.musicstore.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.musicstore.dto.UserDTO;
import com.musicstore.dto.ArtistDTO;
import com.musicstore.dto.AlbumDTO;
import com.musicstore.dto.ReviewDTO;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    public List<Review> getReviewsByAlbumId(Long albumId) {
        return reviewRepository.findByAlbum_Id(albumId);
    }

    public Optional<Review> getReviewById(Long albumId, Long reviewId) {
        return reviewRepository.findById(reviewId);
    }

    public Review addReview(Long albumId, Review review) {
        return reviewRepository.save(review);
    }

    public Review updateReview(Long albumId, Review updatedReview) {
        return reviewRepository.save(updatedReview);
    }

    public void deleteReview(Long albumId, Long reviewId) {
        reviewRepository.deleteById(reviewId);
    }

    public List<Review> getReviewsByUserId(Long userId) {
        return reviewRepository.findByUser_Id(userId);
    }
}
