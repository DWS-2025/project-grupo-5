package com.echoreviews.service;

import com.echoreviews.dto.ArtistDTO;
import com.echoreviews.model.Album;
import com.echoreviews.model.Artist;
import com.echoreviews.model.User;
import com.echoreviews.repository.AlbumRepository;
import com.echoreviews.repository.ArtistRepository;
import com.echoreviews.dto.AlbumDTO;
import com.echoreviews.mapper.AlbumMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
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
        Album probe = new Album();
        if (title != null && !title.trim().isEmpty()) {
            probe.setTitle(title.trim());
        }
        if (year != null) {
            probe.setYear(year);
        }

        ExampleMatcher matcher = ExampleMatcher.matching()
            .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
            .withIgnoreCase()
            .withIgnoreNullValues();

        Example<Album> example = Example.of(probe, matcher);
        List<Album> results = albumRepository.findAll(example);

        // Filter by artist if specified
        if (artist != null && !artist.trim().isEmpty()) {
            results = results.stream()
                .filter(album -> album.getArtists().stream()
                    .anyMatch(a -> a.getName().toLowerCase().contains(artist.trim().toLowerCase())))
                .toList();
        }

        return albumMapper.toDTOList(results);
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

        // Process artists if they exist
        if (albumDTO.artistIds() != null && !albumDTO.artistIds().isEmpty()) {
            List<Artist> processedArtists = albumDTO.artistIds().stream()
                .map(artistId -> artistRepository.findById(artistId).orElse(null))
                .filter(artist -> artist != null)
                .collect(Collectors.toList());
            album.setArtists(processedArtists);
        } else if (albumDTO.artistNames() != null && !albumDTO.artistNames().isEmpty()) {
            List<Artist> processedArtists = albumDTO.artistNames().stream()
                .map(name -> {
                    return artistRepository.findByNameContainingIgnoreCase(name.trim())
                        .stream()
                        .findFirst()
                        .orElseGet(() -> artistRepository.save(new Artist(name.trim())));
                })
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
                album.setAudioPreview(audioFile.getBytes());
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

    public List<AlbumDTO> getTopLikedAlbums() {
        return albumMapper.toDTOList(
            albumRepository.findTop10ByOrderByFavoriteUsersDesc()
                .stream()
                .limit(10)
                .collect(Collectors.toList())
        );
    }

    public List<AlbumDTO> getTopRatedAlbums() {
        return albumMapper.toDTOList(
            albumRepository.findTop10ByOrderByAverageRatingDesc()
                .stream()
                .limit(10)
                .collect(Collectors.toList())
        );
    }
}
