package com.musicstore.dto;

import com.musicstore.model.Album;
import com.musicstore.model.Artist;
import com.musicstore.model.Review;
import java.util.List;
import java.util.stream.Collectors;

public record AlbumDTO(
    Long id,
    String title,
    String genre,
    String imageUrl,
    String description,
    String tracklist,
    Integer year,
    String spotify_url,
    String applemusic_url,
    String tidal_url,
    Double averageRating,
    List<Long> artistIds,
    List<Long> reviewIds,
    List<String> artistNames
) {
    public static AlbumDTO fromAlbum(Album album) {
        return new AlbumDTO(
            album.getId(),
            album.getTitle(),
            album.getGenre(),
            album.getImageUrl(),
            album.getDescription(),
            album.getTracklist(),
            album.getYear(),
            album.getSpotify_url(),
            album.getApplemusic_url(),
            album.getTidal_url(),
            album.getAverageRating(),
            album.getArtists().stream()
                .map(Artist::getId)
                .collect(Collectors.toList()),
            album.getReviews().stream()
                .map(Review::getId)
                .collect(Collectors.toList()),
            album.getArtists().stream()
                .map(Artist::getName)
                .collect(Collectors.toList())
        );
    }

    public Album toAlbum() {
        Album album = new Album();
        album.setId(this.id());
        album.setTitle(this.title());
        album.setGenre(this.genre());
        album.setImageUrl(this.imageUrl());
        album.setDescription(this.description());
        album.setTracklist(this.tracklist());
        album.setYear(this.year());
        album.setSpotify_url(this.spotify_url());
        album.setApplemusic_url(this.applemusic_url());
        album.setTidal_url(this.tidal_url());
        album.setAverageRating(this.averageRating());
        return album;
    }
}