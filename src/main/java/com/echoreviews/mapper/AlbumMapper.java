package com.echoreviews.mapper;

import com.echoreviews.dto.AlbumDTO;
import com.echoreviews.model.Album;
import com.echoreviews.model.Artist;
import com.echoreviews.model.Review;
import com.echoreviews.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import com.echoreviews.dto.ReviewDTO;


import java.util.List;
import java.util.stream.Collectors;
import java.io.IOException;

@Mapper(componentModel = "spring", uses = {ArtistMapper.class, ReviewMapper.class})
public interface AlbumMapper {
    @Mapping(target = "artistIds", source = "artists", qualifiedByName = "artistsToIds")
    @Mapping(target = "artistNames", source = "artists", qualifiedByName = "artistsToNames")
    @Mapping(target = "reviews", source = "reviews", qualifiedByName = "reviewsToDTOs")
    AlbumDTO toDTO(Album album);

    @Mapping(target = "artists", source = "artistIds", qualifiedByName = "idsToArtists")
    @Mapping(target = "reviews", source = "reviews", qualifiedByName = "dtosToReviews")
    Album toEntity(AlbumDTO albumDTO);

    List<AlbumDTO> toDTOList(List<Album> albums);

    List<Album> toEntityList(List<AlbumDTO> albumDTOs);

    @Named("artistsToIds")
    default List<Long> artistsToIds(List<Artist> artists) {
        if (artists == null) {
            return null;
        }
        return artists.stream()
                .map(Artist::getId)
                .collect(Collectors.toList());
    }

    @Named("artistsToNames")
    default List<String> artistsToNames(List<Artist> artists) {
        if (artists == null) {
            return null;
        }
        return artists.stream()
                .map(Artist::getName)
                .collect(Collectors.toList());
    }

    @Named("idsToArtists")
    default List<Artist> idsToArtists(List<Long> artistIds) {
        if (artistIds == null) {
            return null;
        }
        return artistIds.stream()
                .map(id -> {
                    Artist artist = new Artist();
                    artist.setId(id);
                    return artist;
                })
                .collect(Collectors.toList());
    }

    default List<String> mapUsersToUsernames(List<User> users) {
        if (users == null) {
            return null;
        }
        return users.stream()
                .map(User::getUsername)
                .collect(Collectors.toList());
    }

    List<User> mapUserIdsToUsers(List<String> userIds);

    User map(String userId);
    
    /**
     * Convierte una cadena JSON a un objeto AlbumDTO
     * @param json String con formato JSON que representa un AlbumDTO
     * @return AlbumDTO construido a partir del JSON
     * @throws IOException si hay errores al parsear el JSON
     */
    default AlbumDTO fromJson(String json) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, AlbumDTO.class);
    }

    @Named("reviewsToDTOs")
    default List<ReviewDTO> reviewsToDTOs(List<Review> reviews) {
        if (reviews == null) {
            return null;
        }
        return reviews.stream()
                .map(ReviewDTO::fromReview)
                .collect(Collectors.toList());
    }

    @Named("dtosToReviews")
    default List<Review> dtosToReviews(List<ReviewDTO> reviewDTOs) {
        if (reviewDTOs == null) {
            return null;
        }
        return reviewDTOs.stream()
                .map(ReviewDTO::toReview)
                .collect(Collectors.toList());
    }
}