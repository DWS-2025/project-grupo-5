package com.musicstore.dto;

import com.musicstore.model.Artist;
import com.musicstore.model.Album;
import java.util.List;
import java.util.stream.Collectors;

public record ArtistDTO(
    Long id,
    String name,
    String country,
    String imageUrl,
    List<Long> albumIds,
    List<String> albumTitles
) {
    public static ArtistDTO fromArtist(Artist artist) {
        return new ArtistDTO(
            artist.getId(),
            artist.getName(),
            artist.getCountry(),
            artist.getImageUrl(),
            artist.getAlbums().stream()
                .map(Album::getId)
                .collect(Collectors.toList()),
            artist.getAlbums().stream()
                .map(Album::getTitle)
                .collect(Collectors.toList())
        );
    }

    public Artist toArtist() {
        Artist artist = new Artist();
        artist.setId(this.id());
        artist.setName(this.name());
        artist.setCountry(this.country());
        artist.setImageUrl(this.imageUrl());
        return artist;
    }
}