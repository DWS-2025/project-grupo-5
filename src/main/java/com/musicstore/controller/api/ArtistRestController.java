package com.musicstore.controller.api;

import com.musicstore.model.Artist;
import com.musicstore.service.ArtistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/artists")
public class ArtistRestController {

    @Autowired
    private ArtistService artistService;

    @GetMapping
    public ResponseEntity<List<Artist>> getAllArtists() {
        List<Artist> artists = artistService.getAllArtists();
        // Clear binary data and break circular references to prevent infinite recursion
        artists.forEach(artist -> {
            artist.setImageData(null);
            if (artist.getAlbums() != null) {
                artist.getAlbums().forEach(album -> {
                    album.setImageData(null);
                    album.setAudioData(null);
                    album.setArtists(null);
                });
            }
        });
        return ResponseEntity.ok(artists);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Artist> getArtistById(@PathVariable Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            return artistService.getArtistById(id)
                    .map(artist -> {
                        // Clear binary data and break circular references
                        artist.setImageData(null);
                        if (artist.getAlbums() != null) {
                            artist.getAlbums().forEach(album -> {
                                album.setImageData(null);
                                album.setAudioData(null);
                                album.setArtists(null);
                            });
                        }
                        return ResponseEntity.ok(artist);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<Artist> getArtistByName(@PathVariable String name) {
        try {
            return artistService.getArtistByName(name)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<Artist> createArtist(@RequestBody Artist artist) {
        try {
            Artist savedArtist = artistService.saveArtist(artist);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedArtist);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Artist> updateArtist(
            @PathVariable Long id,
            @RequestBody Artist artist) {
        try {
            return (ResponseEntity<Artist>) artistService.getArtistById(id)
                    .map(existingArtist -> {
                        artist.setId(id);
                        try {
                            Artist updatedArtist = artistService.updateArtist(artist);
                            return ResponseEntity.ok(updatedArtist);
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
    public ResponseEntity<Void> deleteArtist(@PathVariable Long id) {
        try {
            artistService.deleteArtist(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<Artist> uploadArtistImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image) {
        try {
            return (ResponseEntity<Artist>) artistService.getArtistById(id)
                    .map(artist -> {
                        try {
                            Artist updatedArtist = artistService.saveArtistWithProfileImage(artist, image);
                            return ResponseEntity.ok(updatedArtist);
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