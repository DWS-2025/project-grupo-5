package com.echoreviews.controller.api;

import com.echoreviews.model.User;
import com.echoreviews.service.UserService;
import com.echoreviews.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import com.echoreviews.dto.UserDTO;

import java.util.Optional;
import java.util.stream.Collectors;
import com.echoreviews.dto.ArtistDTO;
import com.echoreviews.dto.AlbumDTO;
import com.echoreviews.dto.ReviewDTO;
import com.echoreviews.mapper.UserMapper;
import com.echoreviews.mapper.AlbumMapper;
import com.echoreviews.mapper.ReviewMapper;
import com.echoreviews.mapper.ArtistMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Map;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import java.io.IOException;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/users")
public class UserRestController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        List<UserDTO> userDTOs = users.stream()
            .map(UserDTO::withoutImageData)
            .collect(Collectors.toList());
        return ResponseEntity.ok(userDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(user.withoutImageData()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username)
                .map(user -> ResponseEntity.ok(user.withoutImageData()))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO) {
        if (userService.getUserByUsername(userDTO.username()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        UserDTO savedUser = userService.saveUser(userDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser.withoutImageData());
    }

    private Object getValueIfPresent(Map<String, Object> updates, String field) {
        if (!updates.containsKey(field) || updates.get(field) == null) {
            return null;
        }
        // Do not allow password changes through the general update endpoint
        if (field.equals("password") || field.equals("pdfPath") || field.equals("imageUrl")) {
            return null;
        }
        return updates.get(field);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates,
            @RequestHeader("Authorization") String authHeader) {
        
        // Verify that the token exists and has the correct format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extract the token
        String token = authHeader.substring(7);

        try {
            // Get the username from the token
            String username = jwtUtil.extractUsername(token);
            
            // Get the requesting user
            UserDTO requestingUser = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get the user to update
            return userService.getUserById(id)
                    .map(userToUpdate -> {
                        // Verify that the user is the same one being updated or is admin
                        if (!userToUpdate.username().equals(username) && !jwtUtil.isAdmin(token)) {
                            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                        }

                        try {
                            // Create a new UserDTO with updated fields
                            UserDTO updatedUserDTO = new UserDTO(
                                id,
                                getValueIfPresent(updates, "username") != null ? 
                                    (String) getValueIfPresent(updates, "username") : userToUpdate.username(),
                                getValueIfPresent(updates, "password") != null ? 
                                    (String) getValueIfPresent(updates, "password") : userToUpdate.password(),
                                getValueIfPresent(updates, "email") != null ? 
                                    (String) getValueIfPresent(updates, "email") : userToUpdate.email(),
                                // Only allow changes to these fields if admin
                                jwtUtil.isAdmin(token) && getValueIfPresent(updates, "isAdmin") != null ? 
                                    (Boolean) getValueIfPresent(updates, "isAdmin") : userToUpdate.isAdmin(),
                                jwtUtil.isAdmin(token) && getValueIfPresent(updates, "potentiallyDangerous") != null ? 
                                    (Boolean) getValueIfPresent(updates, "potentiallyDangerous") : userToUpdate.potentiallyDangerous(),
                                jwtUtil.isAdmin(token) && getValueIfPresent(updates, "banned") != null ? 
                                    (Boolean) getValueIfPresent(updates, "banned") : userToUpdate.banned(),
                                getValueIfPresent(updates, "imageUrl") != null ? 
                                    (String) getValueIfPresent(updates, "imageUrl") : userToUpdate.imageUrl(),
                                userToUpdate.imageData(), // Cannot update imageData directly
                                userToUpdate.followers(), // Cannot update followers directly
                                userToUpdate.following(), // Cannot update following directly
                                userToUpdate.favoriteAlbumIds() // Cannot update favorites directly
                            );
                            
                            UserDTO savedUser = userService.saveUser(updatedUserDTO);
                            return ResponseEntity.ok(savedUser.withoutImageData());
                        } catch (ClassCastException e) {
                            return ResponseEntity.badRequest()
                                .body(Map.of("error", "Invalid data type for one or more fields"));
                        } catch (RuntimeException e) {
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                        }
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteUser(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        
        // Verify that the token exists and has the correct format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extract the token
        String token = authHeader.substring(7);

        try {
            // Get the username from the token
            String username = jwtUtil.extractUsername(token);
            
            // Get the requesting user
            UserDTO requestingUser = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get the user to delete
            return userService.getUserById(id)
                    .map(userToDelete -> {
                        // Verify that the user is the same one being deleted or is admin
                        if (!userToDelete.username().equals(username) && !jwtUtil.isAdmin(token)) {
                            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                        }

                        try {
                            userService.deleteUser(userToDelete.username());
                            return ResponseEntity.noContent().build();
                        } catch (RuntimeException e) {
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                        }
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<UserDTO> uploadUserImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image) {
        return userService.getUserById(id)
                .map(user -> {
                    try {
                        UserDTO updatedUser = user.withImageData(image.getBytes());
                        UserDTO savedUser = userService.saveUser(updatedUser);
                        return ResponseEntity.ok(savedUser);
                    } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<UserDTO>build();
                    }
                })
                .orElseGet(() -> ResponseEntity.of(Optional.<UserDTO>empty()));
    }

    @GetMapping("/{username}/followers")
    public ResponseEntity<List<UserDTO>> getUserFollowers(@PathVariable String username) {
        try {
            Optional<UserDTO> userOpt = userService.getUserByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            List<UserDTO> followers = userOpt.get().followers().stream()
                    .map(userId -> userService.getUserById(userId))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(UserDTO::withoutImageData)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(followers);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{username}/following")
    public ResponseEntity<List<UserDTO>> getUserFollowing(@PathVariable String username) {
        try {
            Optional<UserDTO> userOpt = userService.getUserByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            List<UserDTO> following = userOpt.get().following().stream()
                    .map(userId -> userService.getUserById(userId))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(UserDTO::withoutImageData)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(following);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Endpoint to follow/unfollow a user.
     * If the user is already being followed, it unfollows them.
     * If not following, it starts following them.
     * 
     * @param targetUserId ID of the user to follow/unfollow
     * @param session HTTP session with authenticated user information
     * @return The updated user after the operation
     */
    @PostMapping("/follow/{targetUserId}")
    public ResponseEntity<?> toggleFollowUser(@PathVariable Long targetUserId, HttpSession session) {
        // Verify if there is an authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        
        try {
            // Get the username of the authenticated user
            String username = "";
            if (auth.getPrincipal() instanceof UserDetails) {
                username = ((UserDetails) auth.getPrincipal()).getUsername();
            } else {
                username = auth.getName();
            }
            
            // Find the user by their username
            UserDTO currentUser = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Current user not found"));
            
            // Get the target user
            UserDTO targetUser = userService.getUserById(targetUserId)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));
            
            // Check if the user is already following the target
            boolean isFollowing = userService.isFollowing(currentUser.id(), targetUserId);
            
            UserDTO updatedUser;
            if (isFollowing) {
                // Unfollow the user
                updatedUser = userService.unfollowUser(currentUser.id(), targetUserId, session);
                return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "action", "unfollow",
                    "message", "You have unfollowed " + targetUser.username()
                ));
            } else {
                // Start following the user
                updatedUser = userService.followUser(currentUser.id(), targetUserId, session);
                return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "action", "follow",
                    "message", "You are now following " + targetUser.username()
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/{id}/change-password")
    public ResponseEntity<?> changePassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> passwordData,
            @RequestHeader("Authorization") String authHeader) {
        
        // Verify that the token exists and has the correct format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extract the token
        String token = authHeader.substring(7);

        try {
            // Get the username from the token
            String username = jwtUtil.extractUsername(token);
            
            // Get the requesting user
            UserDTO requestingUser = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Verify that the required fields are present
            if (!passwordData.containsKey("currentPassword") || !passwordData.containsKey("newPassword")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Both currentPassword and newPassword are required"));
            }

            String currentPassword = passwordData.get("currentPassword");
            String newPassword = passwordData.get("newPassword");

            // Get the user to update
            return userService.getUserById(id)
                    .map(userToUpdate -> {
                        // Verify that the user is the same one being updated or is admin
                        if (!userToUpdate.username().equals(username) && !jwtUtil.isAdmin(token)) {
                            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                        }

                        try {
                            // If it's the same user, verify the current password
                            if (userToUpdate.username().equals(username)) {
                                // Verify the current password using the authentication service
                                if (!userService.verifyPassword(userToUpdate.username(), currentPassword)) {
                                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of("error", "Current password is incorrect"));
                                }
                            }

                            // Update the password using the specific method
                            userService.updatePassword(userToUpdate.id(), newPassword);

                            // Generate a new token after password change
                            UserDetails userDetails = userService.loadUserByUsername(userToUpdate.username());
                            String newToken = jwtUtil.generateToken(userDetails, userToUpdate.isAdmin());

                            return ResponseEntity.ok()
                                .body(Map.of(
                                    "message", "Password updated successfully",
                                    "token", newToken
                                ));
                        } catch (IllegalArgumentException e) {
                            return ResponseEntity.badRequest()
                                .body(Map.of("error", e.getMessage()));
                        } catch (RuntimeException e) {
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", e.getMessage()));
                        }
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint para actualizar un usuario con imagen usando multipart/form-data
     * @param id ID del usuario a actualizar
     * @param userJson Los datos del usuario en formato JSON como string
     * @param image La imagen del usuario (opcional)
     * @param authHeader El token de autenticación
     * @return El usuario actualizado
     */
    @RequestMapping(
        value = "/{id}/with-image", 
        method = {RequestMethod.POST, RequestMethod.PUT},
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UserDTO> updateUserWithImage(
            @PathVariable Long id,
            @RequestPart("user") String userJson,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestHeader("Authorization") String authHeader) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtUtil.extractUsername(token);
            
            UserDTO requestingUser = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            if (!requestingUser.id().equals(id) && !requestingUser.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            UserDTO userToUpdate = userService.getUserById(id)
                    .orElseThrow(() -> new RuntimeException("Usuario a actualizar no encontrado"));
            
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> updates = objectMapper.readValue(userJson, Map.class);
                
                UserDTO updatedUserDTO = new UserDTO(
                    id,
                    getValueIfPresent(updates, "username") != null ? 
                        (String) getValueIfPresent(updates, "username") : userToUpdate.username(),
                    null, // No actualizar la contraseña a través de esta API
                    getValueIfPresent(updates, "email") != null ? 
                        (String) getValueIfPresent(updates, "email") : userToUpdate.email(),
                    requestingUser.isAdmin() && getValueIfPresent(updates, "isAdmin") != null ?
                        (Boolean) getValueIfPresent(updates, "isAdmin") : userToUpdate.isAdmin(),
                    requestingUser.isAdmin() && getValueIfPresent(updates, "potentiallyDangerous") != null ? 
                        (Boolean) getValueIfPresent(updates, "potentiallyDangerous") : userToUpdate.potentiallyDangerous(),
                    requestingUser.isAdmin() && getValueIfPresent(updates, "banned") != null ? 
                        (Boolean) getValueIfPresent(updates, "banned") : userToUpdate.banned(),
                    userToUpdate.imageUrl(),
                    userToUpdate.imageData(),
                    userToUpdate.followers(),
                    userToUpdate.following(),
                    userToUpdate.favoriteAlbumIds(),
                    userToUpdate.pdfPath()
                );
                
                if (image != null && !image.isEmpty()) {
                    validateImageFile(image);
                    
                    String imageUrl = "/api/users/" + id + "/image";
                    
                    updatedUserDTO = updatedUserDTO.withImageData(image.getBytes())
                                                 .withImageUrl(imageUrl);
                }
                
                UserDTO result = userService.saveUser(updatedUserDTO);
                return ResponseEntity.ok(result);
                
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
