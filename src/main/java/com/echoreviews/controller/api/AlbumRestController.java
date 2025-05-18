package com.echoreviews.controller.api;

import com.echoreviews.model.Album;
import com.echoreviews.service.AlbumService;
import com.echoreviews.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import com.echoreviews.dto.UserDTO;
import com.echoreviews.dto.ArtistDTO;
import com.echoreviews.dto.AlbumDTO;
import com.echoreviews.dto.ReviewDTO;
import com.echoreviews.mapper.UserMapper;
import com.echoreviews.mapper.AlbumMapper;
import com.echoreviews.mapper.ReviewMapper;
import com.echoreviews.mapper.ArtistMapper;
import java.util.Map;
import java.util.HashMap;
import com.echoreviews.service.UserService;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/albums")
public class AlbumRestController {

    @Autowired
    private AlbumService albumService;

    @Autowired
    private AlbumMapper albumMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllAlbums(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Album> pageResult = albumService.getAllAlbumsPaged(page, size);
        List<AlbumDTO> albums = albumMapper.toDTOList(pageResult.getContent());

        Map<String, Object> response = new HashMap<>();
        response.put("albums", albums);
        response.put("currentPage", pageResult.getNumber());
        response.put("totalItems", pageResult.getTotalElements());
        response.put("totalPages", pageResult.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlbumDTO> getAlbumById(@PathVariable Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            return albumService.getAlbumById(id)
                    .map(album -> ResponseEntity.ok(albumMapper.toDTO(album.toAlbum())))
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<AlbumDTO> createAlbum(@RequestBody AlbumDTO albumDTO, @RequestHeader("Authorization") String authHeader) {
        // Verificar que el token existe y tiene el formato correcto
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extraer el token
        String token = authHeader.substring(7);

        // Verificar si el usuario es admin
        try {
            if (!jwtUtil.isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            AlbumDTO savedAlbum = albumService.saveAlbum(albumDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedAlbum);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlbumDTO> updateAlbum(
            @PathVariable Long id,
            @RequestBody AlbumDTO albumDTO,
            @RequestHeader("Authorization") String authHeader) {
        
        // Verificar que el token existe y tiene el formato correcto
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extraer el token
        String token = authHeader.substring(7);

        // Verificar si el usuario es admin
        try {
            if (!jwtUtil.isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            return (ResponseEntity<AlbumDTO>) albumService.getAlbumById(id)
                    .map(existingAlbum -> {
                        AlbumDTO updatedAlbumDTO = albumDTO.withId(id);
                        try {
                            AlbumDTO savedAlbum = albumService.saveAlbum(updatedAlbumDTO);
                            return ResponseEntity.ok(savedAlbum);
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
    public ResponseEntity<Void> deleteAlbum(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        // Verificar que el token existe y tiene el formato correcto
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extraer el token
        String token = authHeader.substring(7);

        // Verificar si el usuario es admin
        try {
            if (!jwtUtil.isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            albumService.deleteAlbum(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<AlbumDTO> uploadAlbumImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image) {
        try {
            return (ResponseEntity<AlbumDTO>) albumService.getAlbumById(id)
                    .map(album -> {
                        try {
                            // Here we are working with the Album entity
                            album.withImageData(image.getBytes()); // Modify the entity directly
                            // Save the entity in the database
                            Album updatedAlbum = albumService.saveAlbum(album).toAlbum();
                            // Convert the entity back to a DTO to return it
                            return ResponseEntity.ok(albumMapper.toDTO(updatedAlbum)); // Respondemos con el DTO
                        } catch (IOException e) {
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                        }
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<AlbumDTO>> searchAlbums(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String artist,
            @RequestParam(required = false) Integer year) {
        List<AlbumDTO> albums = albumService.searchAlbums(title, artist, year);
        return ResponseEntity.ok(albums);
    }

    @GetMapping("/favorites/{username}")
    public ResponseEntity<List<AlbumDTO>> getUserFavorites(@PathVariable String username) {
        try {
            List<Long> favoriteAlbumIds = userService.getFavoriteAlbums(username);
            List<AlbumDTO> favoriteAlbums = favoriteAlbumIds.stream()
                    .map(albumService::getAlbumById)
                    .filter(java.util.Optional::isPresent)
                    .map(java.util.Optional::get)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(favoriteAlbums);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/top/liked")
    public ResponseEntity<List<AlbumDTO>> getTopLikedAlbums() {
        try {
            List<AlbumDTO> topAlbums = albumService.getTopLikedAlbums();
            return ResponseEntity.ok(topAlbums);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/top/rated")
    public ResponseEntity<List<AlbumDTO>> getTopRatedAlbums() {
        try {
            List<AlbumDTO> topAlbums = albumService.getTopRatedAlbums();
            return ResponseEntity.ok(topAlbums);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}