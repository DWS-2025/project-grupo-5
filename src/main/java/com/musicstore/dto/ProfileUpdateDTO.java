package com.musicstore.dto;

import org.springframework.web.multipart.MultipartFile;

/**
 * DTO específico para la actualización de perfil de usuario.
 * Contiene solo los campos que pueden ser modificados por el usuario.
 */
public record ProfileUpdateDTO(
    String username,
    String email,
    String currentPassword,
    String newPassword,
    String confirmPassword
) {
    /**
     * Valida que los datos de actualización de perfil sean correctos.
     * @return true si los datos son válidos, false en caso contrario
     */
    public boolean isPasswordChangeValid() {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return true; // No se está intentando cambiar la contraseña
        }
        
        // Verificar que se proporcionó la contraseña actual
        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            return false;
        }
        
        // Verificar que las nuevas contraseñas coinciden
        return newPassword.equals(confirmPassword);
    }
    
    /**
     * Determina si se está intentando cambiar la contraseña.
     * @return true si se está intentando cambiar la contraseña, false en caso contrario
     */
    public boolean isPasswordChangeRequested() {
        return newPassword != null && !newPassword.trim().isEmpty();
    }
}