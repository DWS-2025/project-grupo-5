package com.musicstore.mapper;

import com.musicstore.dto.AlbumDTO;
import com.musicstore.model.Album;
import com.musicstore.model.Artist;
import com.musicstore.model.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AlbumMapper {
    @Mapping(target = "artistIds", expression = "java(album.getArtists().stream().map(artist -> artist.getId()).collect(java.util.stream.Collectors.toList()))")
    @Mapping(target = "artistNames", expression = "java(album.getArtists().stream().map(artist -> artist.getName()).collect(java.util.stream.Collectors.toList()))")
    @Mapping(target = "reviewIds", expression = "java(album.getReviews().stream().map(review -> review.getId()).collect(java.util.stream.Collectors.toList()))")
    AlbumDTO toDTO(Album album);

    @Mapping(target = "artists", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    Album toEntity(AlbumDTO albumDTO);

    List<AlbumDTO> toDTOList(List<Album> albums);
    List<Album> toEntityList(List<AlbumDTO> albumDTOs);
}