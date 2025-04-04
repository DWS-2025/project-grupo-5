package com.musicstore.mapper;

import com.musicstore.dto.ArtistDTO;
import com.musicstore.model.Artist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ArtistMapper {
    @Mapping(target = "albumIds", expression = "java(artist.getAlbums().stream().map(album -> album.getId()).collect(java.util.stream.Collectors.toList()))")
    @Mapping(target = "albumTitles", expression = "java(artist.getAlbums().stream().map(album -> album.getTitle()).collect(java.util.stream.Collectors.toList()))")
    ArtistDTO toDTO(Artist artist);

    @Mapping(target = "albums", ignore = true)
    Artist toEntity(ArtistDTO artistDTO);

    List<ArtistDTO> toDTOList(List<Artist> artists);
    List<Artist> toEntityList(List<ArtistDTO> artistDTOs);
}