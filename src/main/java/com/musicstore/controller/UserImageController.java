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

@RestController
@RequestMapping("/api/users")
public class UserImageController {

    @Autowired
    private UserService userService;

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getAlbumImage(@PathVariable Long id) {
        Optional<User> userOpt = userService.getUserById(id);

        if (userOpt.isPresent() && userOpt.get().getImageData() != null) {
            byte[] imageBytes = userOpt.get().getImageData();
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(imageBytes);
        }

        return ResponseEntity.notFound().build();
    }
}