package com.musicstore.controller.api;

import com.musicstore.model.Album;
import com.musicstore.service.AlbumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/albums")
public class AlbumRestController {

    @Autowired
    private AlbumService albumService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllAlbums(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Album> pageAlbums = albumService.getAlbumsPaginated(page, size);
        List<Album> albums = pageAlbums.getContent();
        
        // Clear binary data and break circular references to prevent infinite recursion
        albums.forEach(album -> {
            album.setImageData(null);
            album.setAudioData(null);
            if (album.getArtists() != null) {
                album.getArtists().forEach(artist -> {
                    artist.setAlbums(null);
                    artist.setImageData(null);
                });
            }
        });

        Map<String, Object> response = new HashMap<>();
        response.put("albums", albums);
        response.put("currentPage", pageAlbums.getNumber());
        response.put("totalItems", pageAlbums.getTotalElements());
        response.put("totalPages", pageAlbums.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Album> getAlbumById(@PathVariable Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            return albumService.getAlbumById(id)
                    .map(album -> {
                        // Clear binary data and break circular references
                        album.setImageData(null);
                        album.setAudioData(null);
                        if (album.getArtists() != null) {
                            album.getArtists().forEach(artist -> {
                                artist.setAlbums(null);
                                artist.setImageData(null);
                            });
                        }
                        return ResponseEntity.ok(album);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<Album> createAlbum(@RequestBody Album album) {
        try {
            Album savedAlbum = albumService.saveAlbum(album);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedAlbum);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Album> updateAlbum(
            @PathVariable Long id,
            @RequestBody Album album) {
        try {
            return (ResponseEntity<Album>) albumService.getAlbumById(id)
                    .map(existingAlbum -> {
                        album.setId(id);
                        try {
                            Album updatedAlbum = albumService.saveAlbum(album);
                            return ResponseEntity.ok(updatedAlbum);
                        } catch (RuntimeException e) {
                            return ResponseEntity.status(HttpStatus.CONFLICT).build();
                        }
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlbum(@PathVariable Long id) {
        try {
            albumService.deleteAlbum(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<Album> uploadAlbumImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image) {
        try {
            return (ResponseEntity<Album>) albumService.getAlbumById(id)
                    .map(album -> {
                        try {
                            album.setImageData(image.getBytes());
                            Album updatedAlbum = albumService.saveAlbum(album);
                            return ResponseEntity.ok(updatedAlbum);
                        } catch (IOException e) {
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                        }
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}