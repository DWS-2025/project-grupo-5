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
     * Endpoint para crear un artista con imagen usando multipart/form-data
     * @param artistJson Los datos del artista en formato JSON como string
     * @param image La imagen del artista (opcional)
     * @param authHeader El token de autenticación
     * @return El artista creado
     */
    @PostMapping(value = "/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ArtistDTO> createArtistWithImage(
            @RequestPart("artist") String artistJson,
            @RequestPart(value = "image", required = false) MultipartFile image,
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
            
            // Convertir el JSON a ArtistDTO
            ArtistDTO artistDTO = objectMapper.readValue(artistJson, ArtistDTO.class);
            
            // Validar datos básicos del artista
            if (artistDTO.name() == null || artistDTO.name().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(null);
            }
            
            // Validar la imagen si se proporcionó
            if (image != null && !image.isEmpty()) {
                // Validación de contenido de imagen
                validateImageFile(image);
                
                // Guardar el artista con la imagen
                ArtistDTO savedArtist = artistService.saveArtistWithProfileImage(artistDTO, image);
                return ResponseEntity.status(HttpStatus.CREATED).body(savedArtist);
            } else {
                // Guardar el artista sin imagen
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
     * Endpoint para actualizar un artista con imagen usando multipart/form-data
     * @param id ID del artista a actualizar
     * @param artistJson Los datos del artista en formato JSON como string
     * @param image La imagen del artista (opcional)
     * @param authHeader El token de autenticación
     * @return El artista actualizado
     */
    @PutMapping(value = "/{id}/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ArtistDTO> updateArtistWithImage(
            @PathVariable Long id,
            @RequestPart("artist") String artistJson,
            @RequestPart(value = "image", required = false) MultipartFile image,
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
            
            // Verificar que el artista existe
            return artistService.getArtistById(id)
                    .map(existingArtist -> {
                        try {
                            // Convertir el JSON a ArtistDTO
                            ArtistDTO artistDTO = objectMapper.readValue(artistJson, ArtistDTO.class);
                            
                            // Asegurar que el ID sea el correcto
                            artistDTO = artistDTO.withId(id);
                            
                            // Validar datos básicos del artista
                            if (artistDTO.name() == null || artistDTO.name().trim().isEmpty()) {
                                return ResponseEntity.badRequest().<ArtistDTO>build();
                            }
                            
                            if (image != null && !image.isEmpty()) {
                                // Validación de contenido de imagen
                                validateImageFile(image);
                                
                                // Actualizar el artista con la imagen
                                ArtistDTO updatedArtist = artistService.saveArtistWithProfileImage(artistDTO, image);
                                return ResponseEntity.ok(updatedArtist);
                            } else {
                                // Mantener la imagen existente si no se proporciona una nueva
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
                                
                                // Guardar la actualización
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
     * Valida una imagen para asegurar que es segura
     * @param image La imagen a validar
     * @throws IOException Si hay errores al procesar la imagen
     * @throws IllegalArgumentException Si la imagen no es válida o segura
     */
    private void validateImageFile(MultipartFile image) throws IOException, IllegalArgumentException {
        // Verificar que no es nulo y tiene contenido
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image file cannot be empty");
        }
        
        // Verificar el tipo de contenido (MIME type)
        String contentType = image.getContentType();
        if (contentType == null || !(contentType.equals("image/jpeg") || 
                                     contentType.equals("image/png") || 
                                     contentType.equals("image/gif") ||
                                     contentType.equals("image/webp"))) {
            throw new IllegalArgumentException("File must be a valid image (JPEG, PNG, GIF or WEBP)");
        }
        
        // Verificar la extensión del archivo
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
        
        // Verificar el tamaño del archivo (máximo 5 MB)
        long maxSizeBytes = 5 * 1024 * 1024; // 5MB
        if (image.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("Image file size must be less than 5MB");
        }
        
        // Validar magic numbers para seguridad adicional
        byte[] bytes = image.getBytes();
        if (bytes.length < 8) { // Las imágenes válidas deberían tener al menos algunos bytes
            throw new IllegalArgumentException("File is too small to be a valid image");
        }
        
        // Verificar los magic numbers de las imágenes comunes
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