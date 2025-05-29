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
import org.springframework.http.MediaType;

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
import org.springframework.util.StringUtils;

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
        List<AlbumDTO> albums = albumMapper.toDTOList(pageResult.getContent()).stream()
            .map(AlbumDTO::withoutImageData)
            .collect(Collectors.toList());

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
                    .map(album -> ResponseEntity.ok(albumMapper.toDTO(album.toAlbum()).withoutImageData()))
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<AlbumDTO> createAlbum(@RequestBody AlbumDTO albumDTO, @RequestHeader("Authorization") String authHeader) {
        // Verify that the token exists and has the correct format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extract the token
        String token = authHeader.substring(7);

        // Verify if the user is admin
        try {
            if (!jwtUtil.isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            AlbumDTO savedAlbum = albumService.saveAlbum(albumDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedAlbum.withoutImageData());
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
        
        // Verify that the token exists and has the correct format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extract the token
        String token = authHeader.substring(7);

        // Verify if the user is admin
        try {
            if (!jwtUtil.isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            return (ResponseEntity<AlbumDTO>) albumService.getAlbumById(id)
                    .map(existingAlbum -> {
                        AlbumDTO updatedAlbumDTO = albumDTO.withId(id);
                        try {
                            AlbumDTO savedAlbum = albumService.saveAlbum(updatedAlbumDTO);
                            return ResponseEntity.ok(savedAlbum.withoutImageData());
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
        // Verify that the token exists and has the correct format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extract the token
        String token = authHeader.substring(7);

        // Verify if the user is admin
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
                            return ResponseEntity.ok(albumMapper.toDTO(updatedAlbum)); // Return the DTO
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
        List<AlbumDTO> albums = albumService.searchAlbums(title, artist, year).stream()
            .map(AlbumDTO::withoutImageData)
            .collect(Collectors.toList());
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
                    .map(AlbumDTO::withoutImageData)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(favoriteAlbums);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/top/liked")
    public ResponseEntity<List<AlbumDTO>> getTopLikedAlbums() {
        try {
            List<AlbumDTO> topAlbums = albumService.getTopLikedAlbums().stream()
                .map(AlbumDTO::withoutImageData)
                .collect(Collectors.toList());
            return ResponseEntity.ok(topAlbums);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/top/rated")
    public ResponseEntity<List<AlbumDTO>> getTopRatedAlbums() {
        try {
            List<AlbumDTO> topAlbums = albumService.getTopRatedAlbums().stream()
                .map(AlbumDTO::withoutImageData)
                .collect(Collectors.toList());
            return ResponseEntity.ok(topAlbums);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<AlbumDTO> addLike(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        
        // Verify that the token exists and has the correct format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extract the token
        String token = authHeader.substring(7);

        try {
            // Get username from token
            String username = jwtUtil.extractUsername(token);
            
            // Get user
            UserDTO user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Add album to favorites
            UserDTO updatedUser = userService.addFavoriteAlbum(user.id(), id, null);
            
            // Get updated album
            return albumService.getAlbumById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<AlbumDTO> removeLike(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        
        // Verify that the token exists and has the correct format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extract the token
        String token = authHeader.substring(7);

        try {
            // Get username from token
            String username = jwtUtil.extractUsername(token);
            
            // Get user
            UserDTO user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Remove album from favorites
            UserDTO updatedUser = userService.deleteFavoriteAlbum(user.id(), id, null);
            
            // Get updated album
            return albumService.getAlbumById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint to create an album with image using multipart/form-data
     * @param albumJson The album data in JSON format as string
     * @param image The album image (optional)
     * @param authHeader The authentication token
     * @return The created album
     */
    @PostMapping(value = "/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AlbumDTO> createAlbumWithImage(
            @RequestPart("album") String albumJson,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestHeader("Authorization") String authHeader) {
        
        // Verify that the token exists and has the correct format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extract the token
        String token = authHeader.substring(7);

        // Verify if the user is admin
        try {
            if (!jwtUtil.isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Convert JSON to AlbumDTO
            AlbumDTO albumDTO = albumMapper.fromJson(albumJson);
            
            // Validate image if provided
            if (image != null && !image.isEmpty()) {
                // Validate image content
                validateImageFile(image);
                
                // Save album with image
                AlbumDTO savedAlbum = albumService.saveAlbumWithImage(albumDTO, image);
                return ResponseEntity.status(HttpStatus.CREATED).body(savedAlbum);
            } else {
                // Save album without image
                AlbumDTO savedAlbum = albumService.saveAlbum(albumDTO);
                return ResponseEntity.status(HttpStatus.CREATED).body(savedAlbum);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }
    
    /**
     * Endpoint to update an album with image using multipart/form-data
     * @param id ID of the album to update
     * @param albumJson The album data in JSON format as string
     * @param image The album image (optional)
     * @param authHeader The authentication token
     * @return The updated album
     */
    @PutMapping(value = "/{id}/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AlbumDTO> updateAlbumWithImage(
            @PathVariable Long id,
            @RequestPart("album") String albumJson,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestHeader("Authorization") String authHeader) {
        
        // Verify that the token exists and has the correct format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extract the token
        String token = authHeader.substring(7);

        // Verify if the user is admin
        try {
            if (!jwtUtil.isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Verify that the album exists
            return albumService.getAlbumById(id)
                    .map(existingAlbum -> {
                        try {
                            // Convert JSON to AlbumDTO
                            AlbumDTO albumDTO = albumMapper.fromJson(albumJson);
                            
                            // Ensure ID is correct
                            albumDTO = albumDTO.withId(id);
                            
                            if (image != null && !image.isEmpty()) {
                                // Validate image content
                                validateImageFile(image);
                                
                                // Update album with image
                                AlbumDTO updatedAlbum = albumService.saveAlbumWithImage(albumDTO, image);
                                return ResponseEntity.ok(updatedAlbum);
                            } else {
                                // Keep existing image if no new one is provided
                                if (existingAlbum.imageData() != null) {
                                    albumDTO = albumDTO.withImageData(existingAlbum.imageData());
                                    albumDTO = albumDTO.withImageUrl(existingAlbum.imageUrl());
                                }
                                
                                // Save the update
                                AlbumDTO updatedAlbum = albumService.saveAlbum(albumDTO);
                                return ResponseEntity.ok(updatedAlbum);
                            }
                        } catch (IOException e) {
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<AlbumDTO>build();
                        } catch (IllegalArgumentException e) {
                            return ResponseEntity.badRequest().<AlbumDTO>build();
                        }
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
    
    /**
     * Validate an image to ensure it is safe
     * @param image The image to validate
     * @throws IOException If there are errors processing the image
     * @throws IllegalArgumentException If the image is not valid or safe
     */
    private void validateImageFile(MultipartFile image) throws IOException, IllegalArgumentException {
        // Verify that it is not null and has content
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image file cannot be empty");
        }
        
        // Verify the content type (MIME type)
        String contentType = image.getContentType();
        if (contentType == null || !(contentType.equals("image/jpeg") || 
                                     contentType.equals("image/png") || 
                                     contentType.equals("image/gif") ||
                                     contentType.equals("image/webp"))) {
            throw new IllegalArgumentException("File must be a valid image (JPEG, PNG, GIF or WEBP)");
        }
        
        // Verify the file extension
        String filename = StringUtils.cleanPath(image.getOriginalFilename());
        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        }
        
        String extension = "";
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = filename.substring(lastDotIndex + 1).toLowerCase();
        }
        
        if (!extension.equals("jpg") && !extension.equals("jpeg") && 
            !extension.equals("png") && !extension.equals("gif") && 
            !extension.equals("webp")) {
            throw new IllegalArgumentException("File must have a valid image extension (jpg, jpeg, png, gif, webp)");
        }
        
        // Verify the file size (maximum 5 MB)
        long maxSizeBytes = 5 * 1024 * 1024; // 5MB
        if (image.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("Image file size must be less than 5MB");
        }
        
        // Validate magic numbers for additional security
        byte[] bytes = image.getBytes();
        if (bytes.length < 8) { // Valid images should have at least some bytes
            throw new IllegalArgumentException("File is too small to be a valid image");
        }
        
        // Verify magic numbers of common images
        // JPEG: FF D8 FF
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        // GIF: 47 49 46 38
        // WEBP: 52 49 46 46 ** ** ** ** 57 45 42 50
        boolean validMagicNumber = false;
        
        if (contentType.equals("image/jpeg") && 
            bytes[0] == (byte) 0xFF && 
            bytes[1] == (byte) 0xD8 && 
            bytes[2] == (byte) 0xFF) {
            validMagicNumber = true;
        } else if (contentType.equals("image/png") && 
                  bytes[0] == (byte) 0x89 && 
                  bytes[1] == (byte) 0x50 && 
                  bytes[2] == (byte) 0x4E && 
                  bytes[3] == (byte) 0x47 && 
                  bytes[4] == (byte) 0x0D && 
                  bytes[5] == (byte) 0x0A && 
                  bytes[6] == (byte) 0x1A && 
                  bytes[7] == (byte) 0x0A) {
            validMagicNumber = true;
        } else if (contentType.equals("image/gif") && 
                  bytes[0] == (byte) 0x47 && 
                  bytes[1] == (byte) 0x49 && 
                  bytes[2] == (byte) 0x46 && 
                  bytes[3] == (byte) 0x38) {
            validMagicNumber = true;
        } else if (contentType.equals("image/webp") && 
                  bytes.length > 12 &&
                  bytes[0] == (byte) 0x52 && 
                  bytes[1] == (byte) 0x49 && 
                  bytes[2] == (byte) 0x46 && 
                  bytes[3] == (byte) 0x46 && 
                  bytes[8] == (byte) 0x57 && 
                  bytes[9] == (byte) 0x45 && 
                  bytes[10] == (byte) 0x42 && 
                  bytes[11] == (byte) 0x50) {
            validMagicNumber = true;
        }
        
        if (!validMagicNumber) {
            throw new IllegalArgumentException("File content does not match its declared image type");
        }
    }
}