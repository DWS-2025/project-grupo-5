package com.echoreviews.service;

import com.echoreviews.model.Artist;
import com.echoreviews.model.Album;
import com.echoreviews.repository.ArtistRepository;
import com.echoreviews.repository.AlbumRepository;
import com.echoreviews.dto.ArtistDTO;
import com.echoreviews.mapper.ArtistMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ArtistService {

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private ArtistMapper artistMapper;

    public List<ArtistDTO> getAllArtists() {
        return artistMapper.toDTOList(artistRepository.findAll());
    }

    public Optional<ArtistDTO> getArtistByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Artist name cannot be null or empty");
        }

        return artistRepository.findByNameContainingIgnoreCase(name.trim())
                .stream()
                .findFirst()
                .map(artistMapper::toDTO);
    }

    public Optional<ArtistDTO> getArtistById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Artist ID cannot be null");
        }

        return artistRepository.findByIdWithAlbums(id)
                .map(artistMapper::toDTO);
    }

    public ArtistDTO saveArtist(ArtistDTO artistDTO) {
        if (artistDTO == null) {
            throw new IllegalArgumentException("Artist cannot be null");
        }

        Artist artist = artistMapper.toEntity(artistDTO);

        if (artist.getId() == null && artistRepository.findByNameContainingIgnoreCase(artist.getName())
                .stream()
                .findFirst()
                .isPresent()) {
            throw new RuntimeException("Artist name already exists");
        }

        return artistMapper.toDTO(artistRepository.save(artist));
    }

    public ArtistDTO saveArtistWithProfileImage(ArtistDTO artistDTO, MultipartFile imageFile) throws IOException {
        if (artistDTO == null || artistDTO.name() == null || artistDTO.name().trim().isEmpty()) {
            throw new IllegalArgumentException("Artist and artist name cannot be null or empty");
        }

        Artist artist = artistMapper.toEntity(artistDTO);

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                artist.setImageData(imageFile.getBytes());
                // Save without ID first, then it will be updated when saving
            } catch (IOException e) {
                throw new RuntimeException("Failed to process image file: " + e.getMessage(), e);
            }
        }

        Artist savedArtist = artistRepository.save(artist);

        if (savedArtist.getImageData() != null) {
            savedArtist.setImageUrl("/api/artists/" + savedArtist.getId() + "/image");
            savedArtist = artistRepository.save(savedArtist); // actualizamos con URL
        }

        return artistMapper.toDTO(savedArtist);
    }

    public ArtistDTO updateArtist(ArtistDTO artistDTO) {
        if (artistDTO == null || artistDTO.id() == null) {
            throw new IllegalArgumentException("Artist or Artist ID cannot be null");
        }

        Long id = artistDTO.id();

        if (!artistRepository.existsById(id)) {
            throw new RuntimeException("Artist not found with ID: " + id);
        }

        Artist artist = artistMapper.toEntity(artistDTO);

        List<Artist> existingArtists = artistRepository.findByNameContainingIgnoreCase(artist.getName());
        boolean nameExists = existingArtists.stream()
                .anyMatch(a -> !a.getId().equals(id));

        if (nameExists) {
            throw new RuntimeException("Artist name already exists for another artist");
        }

        return artistMapper.toDTO(artistRepository.save(artist));
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
