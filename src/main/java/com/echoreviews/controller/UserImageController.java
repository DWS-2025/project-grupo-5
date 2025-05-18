package com.echoreviews.controller;

import com.echoreviews.dto.UserDTO;
import com.echoreviews.service.UserService;
import com.echoreviews.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserImageController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getUserImage(@PathVariable Long id) {
        Optional<UserDTO> userOpt = userService.getUserById(id);

        if (userOpt.isPresent() && userOpt.get().imageData() != null) {
            byte[] imageBytes = userOpt.get().imageData();

            // Detect image type based on URL
            String imageUrl = userOpt.get().imageUrl();
            MediaType contentType = MediaType.IMAGE_JPEG; // default

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

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadUserImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image,
            @RequestHeader("Authorization") String authHeader) {
        
        // Verify token exists and has correct format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extract token
        String token = authHeader.substring(7);
        
        try {
            // Get the user making the request
            String username = jwtUtil.extractUsername(token);
            Optional<UserDTO> requestingUser = userService.getUserByUsername(username);
            
            if (requestingUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Get the target user
            Optional<UserDTO> targetUser = userService.getUserById(id);
            if (targetUser.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // Check if user is authorized (admin or owner of the profile)
            if (!jwtUtil.isAdmin(token) && !requestingUser.get().id().equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Validate image
            validateImageFile(image);
            
            // Create a new UserDTO with only the image data updated
            UserDTO userToUpdate = targetUser.get();
            UserDTO updatedUser = new UserDTO(
                userToUpdate.id(),
                userToUpdate.username(),
                userToUpdate.password(),
                userToUpdate.email(),
                userToUpdate.isAdmin(),
                userToUpdate.potentiallyDangerous(),
                userToUpdate.banned(),
                "/api/users/" + id + "/image", // Update image URL
                image.getBytes(), // Update image data
                userToUpdate.followers(),
                userToUpdate.following(),
                userToUpdate.favoriteAlbumIds(),
                userToUpdate.pdfPath()
            );
            
            // Save the updated user
            UserDTO savedUser = userService.saveUser(updatedUser);
            
            return ResponseEntity.ok()
                .body(savedUser);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing image file: " + e.getMessage());
        }
    }

    /**
     * Validates an image to ensure it is safe
     * @param image The image to validate
     * @throws IOException If there are errors processing the image
     * @throws IllegalArgumentException If the image is not valid or safe
     */
    private void validateImageFile(MultipartFile image) throws IOException, IllegalArgumentException {
        // Verify not null and has content
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image file cannot be empty");
        }
        
        // Verify content type (MIME type)
        String contentType = image.getContentType();
        if (contentType == null || !(contentType.equals("image/jpeg") || 
                                   contentType.equals("image/png") || 
                                   contentType.equals("image/gif") ||
                                   contentType.equals("image/webp"))) {
            throw new IllegalArgumentException("File must be a valid image (JPEG, PNG, GIF or WEBP)");
        }
        
        // Verify file extension
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
        
        // Verify file size (max 5 MB)
        long maxSizeBytes = 5 * 1024 * 1024;
        if (image.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("Image file size must be less than 5MB");
        }
        
        // Validate magic numbers for additional security
        byte[] bytes = image.getBytes();
        if (bytes.length < 8) {
            throw new IllegalArgumentException("File is too small to be a valid image");
        }
        
        // Check magic numbers for common image formats
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
