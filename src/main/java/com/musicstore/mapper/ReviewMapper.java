package com.musicstore.mapper;

import com.musicstore.dto.ReviewDTO;
import com.musicstore.model.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    @Mapping(source = "review.album.title", target = "albumTitle")
    @Mapping(source = "review.album.imageUrl", target = "albumImageUrl")
    @Mapping(source = "user.imageUrl", target = "userImageUrl")
    ReviewDTO toDTO(Review review);
    
    Review toEntity(ReviewDTO reviewDTO);
    List<ReviewDTO> toDTOList(List<Review> reviews);
    List<Review> toEntityList(List<ReviewDTO> reviewDTOs);
}