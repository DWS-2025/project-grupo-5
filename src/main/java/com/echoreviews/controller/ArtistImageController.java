package com.echoreviews.controller;

import com.echoreviews.model.Artist;
import com.echoreviews.service.ArtistService;
import com.echoreviews.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Blob;
import java.sql.SQLException;
import java.util.Optional;
import com.echoreviews.dto.UserDTO;
import com.echoreviews.dto.ArtistDTO;
import com.echoreviews.dto.AlbumDTO;
import com.echoreviews.dto.ReviewDTO;
import com.echoreviews.mapper.UserMapper;
import com.echoreviews.mapper.AlbumMapper;
import com.echoreviews.mapper.ReviewMapper;
import com.echoreviews.mapper.ArtistMapper;

@RestController
@RequestMapping("/api/artists")
public class ArtistImageController {

    @Autowired
    private ArtistService artistService;

    @Autowired
    private ArtistMapper artistMapper;

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getArtistImage(@PathVariable Long id) {
        Optional<ArtistDTO> artistOpt = artistService.getArtistById(id);

        if (artistOpt.isPresent() && artistOpt.get().imageData() != null) {
            byte[] imageBytes = artistOpt.get().imageData();
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(imageBytes);
        }

        return ResponseEntity.notFound().build();
    }
}