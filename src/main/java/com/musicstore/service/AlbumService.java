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
    private AlbumRepository albumRepository;

    public List<Album> getAllAlbums() {
        return albumRepository.findAll();
    }

    public Optional<Album> getAlbumById(Long id) {
        return albumRepository.findById(id);
    }

    public Album saveAlbum(Album album) {
        return albumRepository.save(album);
    }

    public void deleteAlbum(Long id) {
        albumRepository.deleteById(id);
    }
}