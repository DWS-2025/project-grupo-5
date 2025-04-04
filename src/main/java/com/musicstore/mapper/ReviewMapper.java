package com.musicstore.mapper;

import com.musicstore.dto.ReviewDTO;
import com.musicstore.model.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    @Mapping(target = "albumId", source = "album.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    ReviewDTO toDTO(Review review);

    @Mapping(target = "album", ignore = true)
    @Mapping(target = "user", ignore = true)
    Review toEntity(ReviewDTO reviewDTO);

    List<ReviewDTO> toDTOList(List<Review> reviews);
    List<Review> toEntityList(List<ReviewDTO> reviewDTOs);
}