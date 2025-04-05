package com.musicstore.mapper;

import com.musicstore.dto.ReviewDTO;
import com.musicstore.model.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    ReviewDTO toDTO(Review review);
    Review toEntity(ReviewDTO reviewDTO);
    List<ReviewDTO> toDTOList(List<Review> reviews);
    List<Review> toEntityList(List<ReviewDTO> reviewDTOs);
}