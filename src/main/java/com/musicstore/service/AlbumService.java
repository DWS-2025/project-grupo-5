package com.musicstore.service;

import com.musicstore.model.Album;
import com.musicstore.repository.AlbumRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.sql.Blob;
import java.sql.SQLException;
import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialException;

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

    public Album saveAlbumWithImage(Album album, MultipartFile imageFile) throws IOException, javax.sql.rowset.serial.SerialException, java.sql.SQLException {
        Album savedAlbum = albumRepository.save(album);
        
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                byte[] bytes = imageFile.getBytes();
                Blob blob = new SerialBlob(bytes);
                savedAlbum.setImageData(blob);
                savedAlbum.setImageUrl("/api/albums/" + savedAlbum.getId() + "/image");
                return albumRepository.save(savedAlbum);
            } catch (IOException e) {
                throw new RuntimeException("Failed to process image file: " + e.getMessage(), e);
            } catch (SerialException e) {
                throw new RuntimeException("Failed to create BLOB from image file: " + e.getMessage(), e);
            } catch (SQLException e) {
                throw new RuntimeException("Database error while processing image: " + e.getMessage(), e);
            }
        }
        return savedAlbum;
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