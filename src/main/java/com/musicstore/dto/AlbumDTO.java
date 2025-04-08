package com.musicstore.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.musicstore.model.Album;
import com.musicstore.model.Artist;
import com.musicstore.model.Review;
import com.musicstore.model.User;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public record AlbumDTO(
        Long id,
        String title,
        String genre,
        String imageUrl,
        String audioFile,
        String description,
        String tracklist,
        Integer year,
        String spotify_url,
        String applemusic_url,
        String tidal_url,
        Double averageRating,
        List<Long> artistIds,
        List<Long> reviewIds,
        List<String> artistNames,
        List<String> favoriteUsers,
        byte[] imageData,
        byte[] audioData
) {
    public double getAverageRating() {
        return averageRating != null ? averageRating : 0.0;
    }
    public AlbumDTO updateAverageRating(List<ReviewDTO> reviews) {
        double newAverageRating;
        if (reviews == null || reviews.isEmpty()) {
            newAverageRating = 0.0;
        } else {
            double sum = reviews.stream()
                    .filter(review -> review != null)
                    .mapToInt(ReviewDTO::rating)
                    .sum();
            newAverageRating = sum / reviews.size();
        }
        return new AlbumDTO(
                id, title, genre, imageUrl, audioFile, description, tracklist, year,
                spotify_url, applemusic_url, tidal_url, newAverageRating,
                artistIds, reviewIds, artistNames, favoriteUsers, imageData, audioData
        );
    }
    @JsonIgnore
    public static AlbumDTO fromAlbum(Album album) {
        if (album == null) return null;

        List<Artist> artists = album.getArtists();
        if (artists == null) artists = new ArrayList<>();

        List<Review> reviews = album.getReviews();
        if (reviews == null) reviews = new ArrayList<>();

        List<User> favoriteUsers = album.getFavoriteUsers();
        if (favoriteUsers == null) favoriteUsers = new ArrayList<>();

        return new AlbumDTO(
                album.getId(),
                album.getTitle(),
                album.getGenre(),
                album.getImageUrl(),
                album.getAudioFile(),
                album.getDescription(),
                album.getTracklist(),
                album.getYear(),
                album.getSpotify_url(),
                album.getApplemusic_url(),
                album.getTidal_url(),
                album.getAverageRating(),
                artists.stream()
                        .map(Artist::getId)
                        .collect(Collectors.toList()),
                reviews.stream()
                        .map(Review::getId)
                        .collect(Collectors.toList()),
                artists.stream()
                        .map(Artist::getName)
                        .collect(Collectors.toList()),
                favoriteUsers.stream()
                        .map(User::getUsername)
                        .collect(Collectors.toList()),
                album.getImageData(),
                album.getAudioData()
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
        // Inicializamos la lista de artistas
        List<Artist> artists = new ArrayList<>();
        if (this.artistIds != null) {
            for (Long artistId : this.artistIds) {
                Artist artist = new Artist();
                artist.setId(artistId);
                artists.add(artist);
            }
        }
        album.setArtists(artists);
        // No asignamos favoriteUsers aquí ya que necesitamos las entidades User completas
        // La lista de favoriteUsers se manejará en el servicio
        return album;
    }

    public List<String> getFavoriteUsers() {
        return favoriteUsers;
    }

    public List<Long> getArtists() {
        return artistIds;
    }
    public byte[] getImageData() {
        return imageData;
    }

    public byte[] getAudioData() {
        return audioData;
    }
    public AlbumDTO withId(Long newId) {
        return new AlbumDTO(
                newId, title, genre, imageUrl, audioFile, description, tracklist, year,
                spotify_url, applemusic_url, tidal_url, averageRating,
                artistIds, reviewIds, artistNames, favoriteUsers, imageData, audioData
        );
    }
    public AlbumDTO withImageData(byte[] newImageData) {
        return new AlbumDTO(
                id, title, genre, imageUrl, audioFile, description, tracklist, year,
                spotify_url, applemusic_url, tidal_url, averageRating,
                artistIds, reviewIds, artistNames, favoriteUsers, newImageData, audioData
        );
    }
}