package com.musicstore.service;

import com.musicstore.dto.ArtistDTO;
import com.musicstore.model.Album;
import com.musicstore.model.Artist;
import com.musicstore.model.User;
import com.musicstore.repository.AlbumRepository;
import com.musicstore.repository.ArtistRepository;
import com.musicstore.dto.AlbumDTO;
import com.musicstore.mapper.AlbumMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AlbumService {

    public List<AlbumDTO> getAlbumsByIds(List<Long> ids) {
    return albumMapper.toDTOList(albumRepository.findAllById(ids));
}

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private AlbumMapper albumMapper;

    public List<AlbumDTO> getAllAlbums() {
        return albumMapper.toDTOList(albumRepository.findAll());
    }

    public Page<Album> getAllAlbumsPaged(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return albumRepository.findAll(pageable);
    }

    public List<AlbumDTO> searchAlbums(String title, String artist, Integer year) {
        if ((title == null || title.trim().isEmpty()) &&
            (artist == null || artist.trim().isEmpty()) &&
            year == null) {
            return getAllAlbums();
        }

        List<Album> result = albumRepository.findAll();

        if (title != null && !title.trim().isEmpty()) {
            result = result.stream()
                .filter(album -> album.getTitle().toLowerCase().contains(title.trim().toLowerCase()))
                .toList();
        }

        if (artist != null && !artist.trim().isEmpty()) {
            result = result.stream()
                .filter(album -> album.getArtists().stream()
                    .anyMatch(a -> a.getName().toLowerCase().contains(artist.trim().toLowerCase())))
                .toList();
        }

        if (year != null) {
            String yearStr = String.valueOf(year);
            result = result.stream()
                .filter(album -> String.valueOf(album.getYear()).startsWith(yearStr))
                .toList();
        }

        return albumMapper.toDTOList(result);
    }

    public Optional<AlbumDTO> getAlbumById(Long id) {
        return albumRepository.findById(id)
                .map(albumMapper::toDTO);
    }

    public AlbumDTO saveAlbum(AlbumDTO albumDTO) {
        if (albumDTO == null) {
            throw new IllegalArgumentException("AlbumDTO cannot be null");
        }

        Album album = albumMapper.toEntity(albumDTO);

        // Procesar artistas, si los hay
        if (album.getArtists() != null && !album.getArtists().isEmpty()) {
            List<Artist> processedArtists = album.getArtists().stream()
                    .map(artist -> artistRepository.findByNameContainingIgnoreCase(artist.getName().trim())
                            .stream()
                            .findFirst()
                            .orElseGet(() -> artistRepository.save(new Artist(artist.getName().trim()))))
                    .collect(Collectors.toList());
            album.setArtists(processedArtists);
        }

        // Save the album in the database
        Album savedAlbum = albumRepository.save(album);

        // Convert the saved album back to DTO and return it
        return albumMapper.toDTO(savedAlbum);
    }


    public AlbumDTO saveAlbumReview(AlbumDTO albumDTO) {
        if (albumDTO == null) {
            throw new IllegalArgumentException("AlbumDTO cannot be null");
        }

        Album album = albumMapper.toEntity(albumDTO);

        return albumMapper.toDTO(albumRepository.save(album));
    }

    public AlbumDTO saveAlbumWithImage(AlbumDTO albumDTO, MultipartFile imageFile) throws IOException {
        if (albumDTO == null) {
            throw new IllegalArgumentException("AlbumDTO cannot be null");
        }

        AlbumDTO savedAlbumDTO = saveAlbum(albumDTO);
        
        if (imageFile != null && !imageFile.isEmpty()) {
            Album album = albumMapper.toEntity(savedAlbumDTO);
            try {
                album.setImageData(imageFile.getBytes());
                album.setImageUrl("/api/albums/" + album.getId() + "/image");
                savedAlbumDTO = albumMapper.toDTO(albumRepository.save(album));
            } catch (IOException e) {
                throw new RuntimeException("Failed to process image file: " + e.getMessage(), e);
            }
        }
        return savedAlbumDTO;
    }

    public AlbumDTO saveAlbumWithAudio(AlbumDTO albumDTO, MultipartFile audioFile) throws IOException {
        if (albumDTO == null) {
            throw new IllegalArgumentException("AlbumDTO cannot be null");
        }

        AlbumDTO savedAlbumDTO = saveAlbum(albumDTO);
        
        if (audioFile != null && !audioFile.isEmpty()) {
            Album album = albumMapper.toEntity(savedAlbumDTO);
            try {
                album.setAudioData(audioFile.getBytes());
                album.setAudioFile("/api/albums/" + album.getId() + "/audio");
                savedAlbumDTO = albumMapper.toDTO(albumRepository.save(album));
            } catch (IOException e) {
                throw new RuntimeException("Failed to process audio file: " + e.getMessage(), e);
            }
        }
        return savedAlbumDTO;
    }

    public void deleteAlbum(Long id) {
        albumRepository.findById(id).ifPresent(album -> {
            album.getFavoriteUsers().forEach(user -> user.getFavoriteAlbums().remove(album));
            album.getFavoriteUsers().clear();
            albumRepository.delete(album);
        });
    }
}
