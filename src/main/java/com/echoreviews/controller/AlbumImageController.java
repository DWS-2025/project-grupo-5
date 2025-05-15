package com.echoreviews.controller;

import com.echoreviews.dto.AlbumDTO;
import com.echoreviews.model.Album;
import com.echoreviews.service.AlbumService;
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
        Optional<AlbumDTO> albumOpt = albumService.getAlbumById(id);
        
        if (albumOpt.isPresent() && albumOpt.get().getImageData() != null) {
            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(albumOpt.get().getImageData());
        }
        
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/audio")
    public ResponseEntity<byte[]> getAlbumAudio(@PathVariable Long id) {
        Optional<AlbumDTO> albumOpt = albumService.getAlbumById(id);

        if (albumOpt.isPresent() && albumOpt.get().getAudioData() != null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(albumOpt.get().getAudioData());
        }

        return ResponseEntity.notFound().build();
    }

}