package com.echoreviews.dto;

import org.springframework.web.multipart.MultipartFile;

/**
 * Specific DTO for user profile updates.
 * Contains only the fields that can be modified by the user.
 */
public record ProfileUpdateDTO(
    String username,
    String email,
    String currentPassword,
    String newPassword,
    String confirmPassword
) {
    /**
     * Validates that the profile update data is correct.
     * @return true if the data is valid, false otherwise
     */
    public boolean isPasswordChangeValid() {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return true; // Not attempting to change the password
        }
        
        // Verify that the current password was provided
        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            return false;
        }
        
        // Verify that the new passwords match
        return newPassword.equals(confirmPassword);
    }
    
    /**
     * Determines if a password change is being attempted.
     * @return true if the password is being changed, false otherwise
     */
    public boolean isPasswordChangeRequested() {
        return newPassword != null && !newPassword.trim().isEmpty();
    }
}