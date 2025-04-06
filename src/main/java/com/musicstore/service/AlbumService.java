package com.musicstore.service;

import com.musicstore.model.Album;
import com.musicstore.model.Artist;
import com.musicstore.model.User;
import com.musicstore.repository.AlbumRepository;
import com.musicstore.repository.ArtistRepository;
import com.musicstore.dto.AlbumDTO;
import com.musicstore.mapper.AlbumMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class AlbumService {

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private AlbumMapper albumMapper;

    public List<AlbumDTO> getAllAlbums() {
        return albumMapper.toDTOList(albumRepository.findAll());
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

        if (album.getArtists() != null && !album.getArtists().isEmpty()) {
            List<Artist> processedArtists = album.getArtists().stream()
                    .map(artist -> {
                        String name = artist.getName().trim();
                        return artistRepository.findByNameContainingIgnoreCase(name)
                                .stream()
                                .findFirst()
                                .orElseGet(() -> artistRepository.save(new Artist(name)));
                    })
                    .toList();
            album.setArtists(processedArtists);
        }

        Album savedAlbum = albumRepository.save(album);
        return albumMapper.toDTO(savedAlbum);
    }

    public AlbumDTO saveAlbumWithImage(AlbumDTO albumDTO, MultipartFile imageFile) throws IOException {
        if (albumDTO == null) {
            throw new IllegalArgumentException("AlbumDTO cannot be null");
        }

        AlbumDTO savedAlbumDTO = saveAlbum(albumDTO);
        Album album = albumMapper.toEntity(savedAlbumDTO);

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                album.setImageData(imageFile.getBytes());
                album.setImageUrl("/api/albums/" + album.getId() + "/image");
                return albumMapper.toDTO(albumRepository.save(album));
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
        Album album = albumMapper.toEntity(savedAlbumDTO);

        if (audioFile != null && !audioFile.isEmpty()) {
            try {
                album.setAudioData(audioFile.getBytes());
                album.setAudioFile("/api/albums/" + album.getId() + "/audio");
                return albumMapper.toDTO(albumRepository.save(album));
            } catch (IOException e) {
                throw new RuntimeException("Failed to process audio file: " + e.getMessage(), e);
            }
        }
        return savedAlbumDTO;
    }

    public void deleteAlbum(Long id) {
        Optional<Album> albumOpt = albumRepository.findById(id);
        if (albumOpt.isPresent()) {
            Album album = albumOpt.get();
            for (User user : album.getFavoriteUsers()) {
                user.getFavoriteAlbums().remove(album);
            }
            album.getFavoriteUsers().clear();
            albumRepository.delete(album);
        }
    }
}
