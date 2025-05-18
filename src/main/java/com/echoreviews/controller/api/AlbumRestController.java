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

    @PostMapping("/{id}/like")
    public ResponseEntity<AlbumDTO> addLike(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        
        // Verificar que el token existe y tiene el formato correcto
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extraer el token
        String token = authHeader.substring(7);

        try {
            // Obtener el username del token
            String username = jwtUtil.extractUsername(token);
            
            // Obtener el usuario
            UserDTO user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Añadir el álbum a favoritos
            UserDTO updatedUser = userService.addFavoriteAlbum(user.id(), id, null);
            
            // Obtener el álbum actualizado
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
        
        // Verificar que el token existe y tiene el formato correcto
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extraer el token
        String token = authHeader.substring(7);

        try {
            // Obtener el username del token
            String username = jwtUtil.extractUsername(token);
            
            // Obtener el usuario
            UserDTO user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Eliminar el álbum de favoritos
            UserDTO updatedUser = userService.deleteFavoriteAlbum(user.id(), id, null);
            
            // Obtener el álbum actualizado
            return albumService.getAlbumById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint para crear un álbum con imagen usando multipart/form-data
     * @param albumJson Los datos del álbum en formato JSON como string
     * @param image La imagen del álbum (opcional)
     * @param authHeader El token de autenticación
     * @return El álbum creado
     */
    @PostMapping(value = "/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AlbumDTO> createAlbumWithImage(
            @RequestPart("album") String albumJson,
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
            
            // Convertir el JSON a AlbumDTO
            AlbumDTO albumDTO = albumMapper.fromJson(albumJson);
            
            // Validar la imagen si se proporcionó
            if (image != null && !image.isEmpty()) {
                // Validación de contenido de imagen
                validateImageFile(image);
                
                // Guardar el álbum con la imagen
                AlbumDTO savedAlbum = albumService.saveAlbumWithImage(albumDTO, image);
                return ResponseEntity.status(HttpStatus.CREATED).body(savedAlbum);
            } else {
                // Guardar el álbum sin imagen
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
     * Endpoint para actualizar un álbum con imagen usando multipart/form-data
     * @param id ID del álbum a actualizar
     * @param albumJson Los datos del álbum en formato JSON como string
     * @param image La imagen del álbum (opcional)
     * @param authHeader El token de autenticación
     * @return El álbum actualizado
     */
    @PutMapping(value = "/{id}/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AlbumDTO> updateAlbumWithImage(
            @PathVariable Long id,
            @RequestPart("album") String albumJson,
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
            
            // Verificar que el álbum existe
            return albumService.getAlbumById(id)
                    .map(existingAlbum -> {
                        try {
                            // Convertir el JSON a AlbumDTO
                            AlbumDTO albumDTO = albumMapper.fromJson(albumJson);
                            
                            // Asegurar que el ID sea el correcto
                            albumDTO = albumDTO.withId(id);
                            
                            if (image != null && !image.isEmpty()) {
                                // Validación de contenido de imagen
                                validateImageFile(image);
                                
                                // Actualizar el álbum con la imagen
                                AlbumDTO updatedAlbum = albumService.saveAlbumWithImage(albumDTO, image);
                                return ResponseEntity.ok(updatedAlbum);
                            } else {
                                // Mantener la imagen existente si no se proporciona una nueva
                                if (existingAlbum.imageData() != null) {
                                    albumDTO = albumDTO.withImageData(existingAlbum.imageData());
                                    albumDTO = albumDTO.withImageUrl(existingAlbum.imageUrl());
                                }
                                
                                // Guardar la actualización
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