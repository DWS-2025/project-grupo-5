package com.echoreviews.controller.api;

import com.echoreviews.service.UserService;
import com.echoreviews.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.echoreviews.security.JwtUtil;

import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminRestController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/users/{id}/toggle-flag")
    public ResponseEntity<?> toggleUserFlag(
            @PathVariable Long id, 
            @RequestBody UserFlagToggleRequest toggleRequest,
            @RequestHeader("Authorization") String authHeader) {
        
        // Verify that the token exists and has the correct format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extract the token
        String token = authHeader.substring(7);
        
        try {

            if (!jwtUtil.isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                    .body("Unauthorized access: You must be an admin to perform this action");
            }
            
            // Get user to modify
            Optional<UserDTO> userOptional = userService.getUserById(id);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                    .body("User not found with ID: " + id);
            }
            
            UserDTO userToUpdate = userOptional.get();
            
            // Update the appropriate flag
            UserDTO updatedUserDTO;
            switch (toggleRequest.getFlagType()) {
                case "dangerous":
                    updatedUserDTO = new UserDTO(
                        userToUpdate.id(),
                        userToUpdate.username(),
                        userToUpdate.password(),
                        userToUpdate.email(),
                        userToUpdate.isAdmin(),
                        toggleRequest.isValue(),
                        userToUpdate.banned(),
                        userToUpdate.imageUrl(),
                        userToUpdate.imageData(),
                        userToUpdate.followers(),
                        userToUpdate.following(),
                        userToUpdate.favoriteAlbumIds(),
                        userToUpdate.pdfPath()
                    );
                    break;
                case "banned":
                    updatedUserDTO = new UserDTO(
                        userToUpdate.id(),
                        userToUpdate.username(),
                        userToUpdate.password(),
                        userToUpdate.email(),
                        userToUpdate.isAdmin(),
                        userToUpdate.potentiallyDangerous(),
                        toggleRequest.isValue(),
                        userToUpdate.imageUrl(),
                        userToUpdate.imageData(),
                        userToUpdate.followers(),
                        userToUpdate.following(),
                        userToUpdate.favoriteAlbumIds(),
                        userToUpdate.pdfPath()
                    );
                    break;
                case "admin":
                    updatedUserDTO = new UserDTO(
                        userToUpdate.id(),
                        userToUpdate.username(),
                        userToUpdate.password(),
                        userToUpdate.email(),
                        toggleRequest.isValue(),
                        userToUpdate.potentiallyDangerous(),
                        userToUpdate.banned(),
                        userToUpdate.imageUrl(),
                        userToUpdate.imageData(),
                        userToUpdate.followers(),
                        userToUpdate.following(),
                        userToUpdate.favoriteAlbumIds(),
                        userToUpdate.pdfPath()
                    );
                    break;
                default:
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body("Invalid flag type: " + toggleRequest.getFlagType());
            }
            
            // Save updated user
            UserDTO savedUser = userService.updateUser(updatedUserDTO);
            
            // Return success response
            return ResponseEntity.ok(savedUser);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Error updating user flag: " + e.getMessage());
        }
    }

    // Inner class to handle flag toggle requests
    public static class UserFlagToggleRequest {
        private String flagType;
        private boolean value;
        
        // Default constructor for JSON deserialization
        public UserFlagToggleRequest() {
        }
        
        // Constructor with fields
        public UserFlagToggleRequest(String flagType, boolean value) {
            this.flagType = flagType;
            this.value = value;
        }
        
        public String getFlagType() {
            return flagType;
        }
        
        public void setFlagType(String flagType) {
            this.flagType = flagType;
        }
        
        public boolean isValue() {
            return value;
        }
        
        public void setValue(boolean value) {
            this.value = value;
        }
        
        @Override
        public String toString() {
            return "UserFlagToggleRequest{" +
                   "flagType='" + flagType + '\'' +
                   ", value=" + value +
                   '}';
        }
    }
} 