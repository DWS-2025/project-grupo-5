package com.musicstore.mapper;

import com.musicstore.dto.ArtistDTO;
import com.musicstore.model.Artist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ArtistMapper {
    ArtistDTO toDTO(Artist artist);
    Artist toEntity(ArtistDTO artistDTO);
    List<ArtistDTO> toDTOList(List<Artist> artists);
    List<Artist> toEntityList(List<ArtistDTO> artistDTOs);
}