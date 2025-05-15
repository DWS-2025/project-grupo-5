package com.echoreviews.dto;

import com.echoreviews.model.Artist;
import com.echoreviews.model.Album;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public record ArtistDTO(
        Long id,
        String name,
        String country,
        String imageUrl,
        List<Long> albumIds,
        List<String> albumTitles,
        byte[] imageData
) {
    public static ArtistDTO fromArtist(Artist artist) {
        return new ArtistDTO(
                artist.getId(),
                artist.getName(),
                artist.getCountry(),
                artist.getImageUrl(),
                artist.getAlbums() != null ? artist.getAlbums().stream()
                        .map(Album::getId)
                        .collect(Collectors.toList()) : Collections.emptyList(),
                artist.getAlbums().stream()
                        .map(Album::getTitle)
                        .collect(Collectors.toList()),
                artist.getImageData()
        );
    }

    public Artist toArtist() {
        Artist artist = new Artist();
        artist.setId(this.id());
        artist.setName(this.name());
        artist.setCountry(this.country());
        artist.setImageUrl(this.imageUrl());
        artist.setImageData(this.imageData());
        return artist;
    }

    public ArtistDTO withId(Long newId) {
        return new ArtistDTO(
                newId,
                this.name,
                this.country,
                this.imageUrl,
                this.albumIds,
                this.albumTitles,
                this.imageData
        );
    }
}