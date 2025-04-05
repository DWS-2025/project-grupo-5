package com.musicstore.service;

import com.musicstore.model.Artist;
import com.musicstore.model.Album;
import com.musicstore.model.User;
import com.musicstore.repository.ArtistRepository;
import com.musicstore.repository.AlbumRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ArtistService {
    @Autowired
    private ArtistRepository artistRepository;
    
    @Autowired
    private AlbumRepository albumRepository;

    public List<Artist> getAllArtists() {
        return artistRepository.findAll();
    }

    public Optional<Artist> getArtistByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Artist name cannot be null or empty");
        }
        return artistRepository.findByNameContainingIgnoreCase(name.trim()).stream().findFirst();
    }

    public Optional<Artist> getArtistById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Artist ID cannot be null");
        }
        return artistRepository.findById(id);
    }

    public Artist saveArtist(Artist artist) {
        if (artist == null) {
            throw new IllegalArgumentException("Artist cannot be null");
        }

        if (artist.getId() == null && artistRepository.findByNameContainingIgnoreCase(artist.getName()).stream().findFirst().isPresent()) {
            throw new RuntimeException("Artist name already exists");
        }

        return artistRepository.save(artist);
    }

    /*
    public void saveArtistWithProfileImage(Artist artist, MultipartFile profileImage) throws IOException {
        if (profileImage != null && !profileImage.isEmpty()) {
            String imageUrl = fileStorageService.storeFile(profileImage);
            artist.setImageUrl(imageUrl);
        }
        saveArtist(artist);
    }*/

    public Artist saveArtistWithProfileImage(Artist artist, MultipartFile imageFile) throws IOException {
        Artist savedArtist = artistRepository.save(artist);

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                byte[] imageData = imageFile.getBytes();
                savedArtist.setImageData(imageData);
                savedArtist.setImageUrl("/api/artists/" + savedArtist.getId() + "/image");
                return artistRepository.save(savedArtist);
            } catch (IOException e) {
                throw new RuntimeException("Failed to process image file: " + e.getMessage(), e);
            }
        }
        return savedArtist;
    }


    public Artist updateArtist(Artist updatedArtist) {
        if (updatedArtist == null || updatedArtist.getId() == null) {
            throw new IllegalArgumentException("Artist or Artist ID cannot be null");
        }

        if (!artistRepository.existsById(updatedArtist.getId())) {
            throw new RuntimeException("Artist not found with ID: " + updatedArtist.getId());
        }

        List<Artist> existingArtists = artistRepository.findByNameContainingIgnoreCase(updatedArtist.getName());
        boolean nameExists = existingArtists.stream()
                .anyMatch(artist -> !artist.getId().equals(updatedArtist.getId()));

        if (nameExists) {
            throw new RuntimeException("Artist name already exists");
        }

        return artistRepository.save(updatedArtist);
    }

    public void deleteArtist(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Artist ID cannot be null");
        }

        Optional<Artist> artistOpt = artistRepository.findById(id);
        if (artistOpt.isEmpty()) {
            throw new RuntimeException("Artist not found with ID: " + id);
        }

        Artist artist = artistOpt.get();
        List<Album> albums = new ArrayList<>(artist.getAlbums());
        
        // Remove artist from all albums and delete albums that have no other artists
        for (Album album : albums) {
            album.removeArtist(artist);
            if (album.getArtists().isEmpty()) {
                albumRepository.delete(album);
            } else {
                albumRepository.save(album);
            }
        }

        artistRepository.delete(artist);
    }
}
