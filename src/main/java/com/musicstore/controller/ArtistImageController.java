package com.musicstore.controller;

import com.musicstore.model.Artist;
import com.musicstore.service.ArtistService;
import com.musicstore.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Blob;
import java.sql.SQLException;
import java.util.Optional;
import com.musicstore.dto.UserDTO;
import com.musicstore.dto.ArtistDTO;
import com.musicstore.dto.AlbumDTO;
import com.musicstore.dto.ReviewDTO;
import com.musicstore.mapper.UserMapper;
import com.musicstore.mapper.AlbumMapper;
import com.musicstore.mapper.ReviewMapper;
import com.musicstore.mapper.ArtistMapper;

@RestController
@RequestMapping("/api/artists")
public class ArtistImageController {

    @Autowired
    private ArtistService artistService;

    @Autowired
    private ArtistMapper artistMapper;

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getArtistImage(@PathVariable Long id) {
        Optional<Artist> artistOpt = artistService.getArtistById(id);

        if (artistOpt.isPresent() && artistOpt.get().getImageData() != null) {
            byte[] imageBytes = artistOpt.get().getImageData();
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(imageBytes);
        }

        return ResponseEntity.notFound().build();
    }
}