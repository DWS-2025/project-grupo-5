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
import java.util.stream.Collectors;
import com.musicstore.dto.ArtistDTO;
import com.musicstore.mapper.ArtistMapper;

@RestController
@RequestMapping("/api/artists")
public class ArtistRestController {

    @Autowired
    private ArtistService artistService;

    @Autowired
    private ArtistMapper artistMapper;

    @GetMapping
    public ResponseEntity<List<ArtistDTO>> getAllArtists() {
        List<Artist> artists = artistService.getAllArtists();
        List<ArtistDTO> artistDTOs = artists.stream()
                .map(artistMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(artistDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArtistDTO> getArtistById(@PathVariable Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            return artistService.getArtistById(id)
                    .map(artist -> ResponseEntity.ok(artistMapper.toDTO(artist)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<ArtistDTO> getArtistByName(@PathVariable String name) {
        try {
            return artistService.getArtistByName(name)
                    .map(artist -> ResponseEntity.ok(artistMapper.toDTO(artist)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<ArtistDTO> createArtist(@RequestBody ArtistDTO artistDTO) {
        try {
            Artist artist = artistMapper.toEntity(artistDTO);
            Artist savedArtist = artistService.saveArtist(artist);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(artistMapper.toDTO(savedArtist));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ArtistDTO> updateArtist(
            @PathVariable Long id,
            @RequestBody ArtistDTO artistDTO) {
        try {
            return (ResponseEntity<ArtistDTO>) artistService.getArtistById(id)
                    .map(existingArtist -> {
                        Artist artistToUpdate = artistMapper.toEntity(artistDTO);
                        artistToUpdate.setId(id);
                        try {
                            Artist updatedArtist = artistService.updateArtist(artistToUpdate);
                            return ResponseEntity.ok(artistMapper.toDTO(updatedArtist));
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
    public ResponseEntity<ArtistDTO> uploadArtistImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image) {
        try {
            return (ResponseEntity<ArtistDTO>) artistService.getArtistById(id)
                    .map(artist -> {
                        try {
                            Artist updatedArtist = artistService.saveArtistWithProfileImage(artist, image);
                            return ResponseEntity.ok(artistMapper.toDTO(updatedArtist));
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