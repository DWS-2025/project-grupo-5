package com.echoreviews.controller.api;

import com.echoreviews.model.Artist;
import com.echoreviews.service.ArtistService;
import com.echoreviews.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.echoreviews.dto.UserDTO;
import com.echoreviews.dto.ArtistDTO;
import com.echoreviews.dto.AlbumDTO;
import com.echoreviews.dto.ReviewDTO;
import com.echoreviews.mapper.UserMapper;
import com.echoreviews.mapper.AlbumMapper;
import com.echoreviews.mapper.ReviewMapper;
import com.echoreviews.mapper.ArtistMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/artists")
public class ArtistRestController {

    @Autowired
    private ArtistService artistService;

    @Autowired
    private ArtistMapper artistMapper;

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<ArtistDTO>> getAllArtists() {
        List<ArtistDTO> artists = artistService.getAllArtists();
        return ResponseEntity.ok(artists);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArtistDTO> getArtistById(@PathVariable Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            return artistService.getArtistById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<ArtistDTO> getArtistByName(@PathVariable String name) {
        try {
            return artistService.getArtistByName(name)
                    .map(artist -> ResponseEntity.ok(artistMapper.toDTO(artist.toArtist())))
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<ArtistDTO> createArtist(@RequestBody ArtistDTO artistDTO, @RequestHeader("Authorization") String authHeader) {
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

            Artist artist = artistMapper.toEntity(artistDTO);
            Artist savedArtist = artistService.saveArtist(ArtistDTO.fromArtist(artist)).toArtist();
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
            @RequestBody ArtistDTO artistDTO,
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

            return (ResponseEntity<ArtistDTO>) artistService.getArtistById(id)
                    .map(existingArtist -> {
                        ArtistDTO artistToUpdate = artistDTO.withId(id);
                        try {
                            ArtistDTO updatedArtist = artistService.updateArtist(artistToUpdate);
                            return ResponseEntity.ok(updatedArtist);
                        } catch (RuntimeException e) {
                            return ResponseEntity.<ArtistDTO>status(HttpStatus.CONFLICT).build();
                        }
                    })
                    .orElseGet(() -> ResponseEntity.<ArtistDTO>notFound().build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArtist(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
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
                            Artist updatedArtist = artistService.saveArtistWithProfileImage(artist, image).toArtist();
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
    
    /**
     * Endpoint to create an artist with image using multipart/form-data
     * @param artistJson The artist data in JSON format as string
     * @param image The artist image (optional)
     * @param authHeader The authentication token
     * @return The created artist
     */
    @PostMapping(value = "/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ArtistDTO> createArtistWithImage(
            @RequestPart("artist") String artistJson,
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
            
            // Convert the JSON to ArtistDTO
            ArtistDTO artistDTO = objectMapper.readValue(artistJson, ArtistDTO.class);
            
            // Validate basic artist data
            if (artistDTO.name() == null || artistDTO.name().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(null);
            }
            
            // Validate the image if provided
            if (image != null && !image.isEmpty()) {
                // Image content validation
                validateImageFile(image);
                
                // Save the artist with the image
                ArtistDTO savedArtist = artistService.saveArtistWithProfileImage(artistDTO, image);
                return ResponseEntity.status(HttpStatus.CREATED).body(savedArtist);
            } else {
                // Save the artist without image
                ArtistDTO savedArtist = artistService.saveArtist(artistDTO);
                return ResponseEntity.status(HttpStatus.CREATED).body(savedArtist);
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
     * Endpoint to update an artist with image using multipart/form-data
     * @param id ID of the artist to update
     * @param artistJson The artist data in JSON format as string
     * @param image The artist image (optional)
     * @param authHeader The authentication token
     * @return The updated artist
     */
    @PutMapping(value = "/{id}/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ArtistDTO> updateArtistWithImage(
            @PathVariable Long id,
            @RequestPart("artist") String artistJson,
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
            
            // Verify that the artist exists
            return artistService.getArtistById(id)
                    .map(existingArtist -> {
                        try {
                            // Convert the JSON to ArtistDTO
                            ArtistDTO artistDTO = objectMapper.readValue(artistJson, ArtistDTO.class);
                            
                            // Ensure the ID is correct
                            artistDTO = artistDTO.withId(id);
                            
                            // Validate basic artist data
                            if (artistDTO.name() == null || artistDTO.name().trim().isEmpty()) {
                                return ResponseEntity.badRequest().<ArtistDTO>build();
                            }
                            
                            if (image != null && !image.isEmpty()) {
                                // Image content validation
                                validateImageFile(image);
                                
                                // Update the artist with the image
                                ArtistDTO updatedArtist = artistService.saveArtistWithProfileImage(artistDTO, image);
                                return ResponseEntity.ok(updatedArtist);
                            } else {
                                // Keep existing image if no new image is provided
                                if (existingArtist.imageData() != null) {
                                    artistDTO = new ArtistDTO(
                                        artistDTO.id(),
                                        artistDTO.name(),
                                        artistDTO.country(),
                                        existingArtist.imageUrl(),
                                        artistDTO.albumIds(),
                                        artistDTO.albumTitles(),
                                        existingArtist.imageData()
                                    );
                                }
                                
                                // Save the update
                                ArtistDTO updatedArtist = artistService.updateArtist(artistDTO);
                                return ResponseEntity.ok(updatedArtist);
                            }
                        } catch (IOException e) {
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<ArtistDTO>build();
                        } catch (IllegalArgumentException e) {
                            return ResponseEntity.badRequest().<ArtistDTO>build();
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
     * @throws IOException If there is an error processing the image
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