package com.musicstore.controller;

import com.musicstore.model.User;
import com.musicstore.service.AlbumService;
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
@RequestMapping("/api/users")
public class UserImageController {

    @Autowired
    private UserService userService;

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getUserImage(@PathVariable Long id) {
        Optional<UserDTO> userOpt = userService.getUserById(id);

        if (userOpt.isPresent() && userOpt.get().imageData() != null) {
            byte[] imageBytes = userOpt.get().imageData();
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(imageBytes);
        }

        return ResponseEntity.notFound().build();
    }
}