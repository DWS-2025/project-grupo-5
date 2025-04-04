package com.musicstore.dto;

import com.musicstore.model.Review;

public record ReviewDTO(
    Long id,
    Long albumId,
    Long userId,
    String username,
    String userImageUrl,
    String albumTitle,
    String albumImageUrl,
    String content,
    int rating
) {
    public static ReviewDTO fromReview(Review review) {
        return new ReviewDTO(
            review.getId(),
            review.getAlbumId(),
            review.getUserId(),
            review.getUsername(),
            review.getUserImageUrl(),
            review.getAlbumTitle(),
            review.getAlbumImageUrl(),
            review.getContent(),
            review.getRating()
        );
    }

    public Review toReview() {
        Review review = new Review();
        review.setId(this.id());
        review.setAlbumId(this.albumId());
        review.setUserId(this.userId());
        review.setUsername(this.username());
        review.setUserImageUrl(this.userImageUrl());
        review.setAlbumTitle(this.albumTitle());
        review.setAlbumImageUrl(this.albumImageUrl());
        review.setContent(this.content());
        review.setRating(this.rating());
        return review;
    }
}