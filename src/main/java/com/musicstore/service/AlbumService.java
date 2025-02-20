package com.musicstore.service;

import com.musicstore.model.Album;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    public Album saveAlbumWithImage(Album album, MultipartFile imageFile) throws IOException {
        if (imageFile != null && !imageFile.isEmpty()) {
            // Guardar la nueva imagen y actualizar el campo imageUrl
            String imageUrl = fileStorageService.storeFile(imageFile);
            album.setImageUrl(imageUrl);
        }
        // Si no se sube una nueva imagen, simplemente no cambies el campo imageUrl

        return fileStorageService.saveAlbum(album);
    }

    public Album saveAlbumWithAudio(Album album, MultipartFile audioFile2) throws IOException {
        if (audioFile2 != null && !audioFile2.isEmpty()) {
            // Guardar el archivo y reemplazar el valor de audioFile
            String audioFile = fileStorageService.storeAudio(audioFile2);
            album.setAudioFile(audioFile); // Sobrescribe el valor de audioFile
        }
        // Si no se sube un nuevo archivo, no se modifica el campo
        return fileStorageService.saveAlbum(album);
    }


    public void deleteAlbum(Long id) {
        fileStorageService.deleteAlbum(id);  // Eliminamos el álbum por ID
    }

}