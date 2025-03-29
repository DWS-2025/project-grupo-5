package com.musicstore.controller;

import com.musicstore.model.Album;
import com.musicstore.service.AlbumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Blob;
import java.sql.SQLException;
import java.util.Optional;

@RestController
@RequestMapping("/api/albums")
public class AlbumImageController {

    @Autowired
    private AlbumService albumService;

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getAlbumImage(@PathVariable Long id) {
        Optional<Album> albumOpt = albumService.getAlbumById(id);
        
        if (albumOpt.isPresent() && albumOpt.get().getImageData() != null) {
            byte[] imageBytes = albumOpt.get().getImageData();

            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(imageBytes);
        }
        
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/audio")
    public ResponseEntity<byte[]> getAlbumAudio(@PathVariable Long id) {
        Optional<Album> albumOpt = albumService.getAlbumById(id);

        if (albumOpt.isPresent() && albumOpt.get().getAudioData() != null) {
            try {
                Blob audioBlob = albumOpt.get().getAudioData();
                byte[] audioBytes = audioBlob.getBytes(1, (int) audioBlob.length());

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("audio/mpeg"))
                        .header("Content-Disposition", "inline")
                        .body(audioBytes);
            } catch (SQLException e) {
                e.printStackTrace();
                return ResponseEntity.internalServerError().build();
            }
        }

        return ResponseEntity.notFound().build();
    }

}