package com.musicstore.service;

import com.musicstore.model.Album;
import com.musicstore.repository.AlbumRepository;
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
    private FileStorageService fileStorageService;

    public List<Album> getAllAlbums() {
        return albumRepository.findAll();
    }

    public Optional<Album> getAlbumById(Long id) {
        return albumRepository.findById(id);
    }

    public Album saveAlbum(Album album) {
        return albumRepository.save(album);
    }

    public Album saveAlbumWithImage(Album album, MultipartFile imageFile) throws IOException {
        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = fileStorageService.storeFile(imageFile);
            album.setImageUrl(imageUrl);
        }

        return albumRepository.save(album);
    }

    public Album saveAlbumWithAudio(Album album, MultipartFile audioFile2) throws IOException {
        if (audioFile2 != null && !audioFile2.isEmpty()) {
            String audioUrl = fileStorageService.storeAudio(audioFile2);
            album.setAudioFile(audioUrl);
        }
        return albumRepository.save(album);
    }

    public void deleteAlbum(Long id) {
        albumRepository.deleteById(id);
    }
}