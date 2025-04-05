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
    AlbumDTO toDTO(Album album);
    Album toEntity(AlbumDTO albumDTO);
    List<AlbumDTO> toDTOList(List<Album> albums);
    List<Album> toEntityList(List<AlbumDTO> albumDTOs);
}