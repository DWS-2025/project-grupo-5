package com.musicstore.service;

import com.musicstore.model.Album;
import com.musicstore.repository.AlbumRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AlbumService {

    @Autowired
    private FileStorageService fileStorageService;

    public List<Album> getAllAlbums() {
        return fileStorageService.getAllAlbums();
    }

    public Optional<Album> getAlbumById(Long id) {
        return fileStorageService.getAlbumById(id);
    }

    public Album saveAlbum(Album album) {
        return fileStorageService.saveAlbum(album);
    }

    public void deleteAlbum(Long id) {
        fileStorageService.deleteAlbum(id);
    }
}