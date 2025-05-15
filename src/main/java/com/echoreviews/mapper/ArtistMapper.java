package com.echoreviews.mapper;

import com.echoreviews.dto.ArtistDTO;
import com.echoreviews.model.Artist;
import com.echoreviews.model.Album;
import java.util.Collections;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ArtistMapper {
    @Named("albumsToIds")
    default List<Long> albumsToIds(List<Album> albums) {
        if (albums == null) return Collections.emptyList();
        return albums.stream().map(Album::getId).collect(Collectors.toList());
    }

    @Named("idsToAlbums")
    default List<Album> idsToAlbums(List<Long> albumIds) {
        if (albumIds == null) return Collections.emptyList();
        return albumIds.stream().map(id -> {
            Album album = new Album();
            album.setId(id);
            return album;
        }).collect(Collectors.toList());
    }

    @Mapping(target = "albumIds", source = "albums", qualifiedByName = "albumsToIds")
    ArtistDTO toDTO(Artist artist);

    @Mapping(target = "albums", source = "albumIds", qualifiedByName = "idsToAlbums")
    Artist toEntity(ArtistDTO artistDTO);


    List<ArtistDTO> toDTOList(List<Artist> artists);
    List<Artist> toEntityList(List<ArtistDTO> artistDTOs);
}