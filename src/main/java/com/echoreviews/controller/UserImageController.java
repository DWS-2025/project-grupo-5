package com.echoreviews.controller;

import com.echoreviews.dto.UserDTO;
import com.echoreviews.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

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

            // Detectamos el tipo de imagen según la URL
            String imageUrl = userOpt.get().imageUrl();
            MediaType contentType = MediaType.IMAGE_JPEG; // por defecto

            if (imageUrl != null) {
                if (imageUrl.endsWith(".png")) {
                    contentType = MediaType.IMAGE_PNG;
                } else if (imageUrl.endsWith(".gif")) {
                    contentType = MediaType.IMAGE_GIF;
                }
            }

            return ResponseEntity.ok()
                    .contentType(contentType)
                    .body(imageBytes);
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .header("X-Error", "No image found for user ID " + id)
                .build();
    }
}
