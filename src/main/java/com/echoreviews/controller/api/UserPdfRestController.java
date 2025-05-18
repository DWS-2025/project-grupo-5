package com.echoreviews.controller.api;

import com.echoreviews.dto.UserDTO;
import com.echoreviews.security.JwtUtil;
import com.echoreviews.service.UserService;
import com.echoreviews.service.UserService.PdfUploadResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserPdfRestController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private ResourceLoader resourceLoader;
    
    // Directorio base para los PDFs (por defecto user.dir para usar el directorio del proyecto)
    @Value("${app.pdf.storage.directory:./user-pdfs}")
    private String pdfBaseDirectory;
    
    private boolean isAuthorized(String token, Long userId) {
        try {
            String username = jwtUtil.extractUsername(token);
            UserDTO requestingUser = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // El usuario está autorizado si es admin o si es el propietario del recurso
            return requestingUser.isAdmin() || requestingUser.id().equals(userId);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Endpoint para subir un archivo PDF a un usuario.
     * El usuario debe estar autenticado y solo puede subir PDF a su propio perfil.
     * 
     * @param userId ID del usuario al que se le asignará el PDF
     * @param pdf El archivo PDF a subir
     * @param authHeader El token de autenticación
     * @return Respuesta con información del resultado
     */
    @PostMapping("/{userId}/pdf")
    public ResponseEntity<Map<String, Object>> uploadUserPdf(
            @PathVariable Long userId,
            @RequestParam("pdf") MultipartFile pdf,
            @RequestHeader("Authorization") String authHeader) {
        
        // Verificar que el token existe y tiene el formato correcto
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "No se proporcionó un token de autenticación válido"));
        }

        String token = authHeader.substring(7);
        
        // Verificar autorización
        if (!isAuthorized(token, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "error", "No tienes permiso para subir archivos PDF a este usuario"));
        }
        
        try {
            // Obtener el usuario al que se le asignará el PDF
            UserDTO targetUser = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("Usuario objetivo no encontrado"));
            
            // Subir el PDF
            PdfUploadResult result = userService.uploadUserPdf(targetUser, pdf);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                    "success", true, 
                    "message", "Archivo PDF subido correctamente",
                    "pdfPath", result.getUser().pdfPath()
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false, 
                    "error", result.getErrorMessage()
                ));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Endpoint para descargar el PDF de un usuario.
     * 
     * @param userId ID del usuario cuyo PDF se quiere descargar
     * @param authHeader El token de autenticación
     * @return El archivo PDF para descargar
     */
    @GetMapping("/{userId}/pdf")
    public ResponseEntity<?> downloadUserPdf(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No se proporcionó un token de autenticación válido"));
        }

        String token = authHeader.substring(7);
        
        // Verificar autorización
        if (!isAuthorized(token, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tienes permiso para descargar el PDF de este usuario"));
        }
        
        try {
            Optional<UserDTO> userOpt = userService.getUserById(userId);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            UserDTO user = userOpt.get();
            
            if (user.pdfPath() == null || user.pdfPath().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "El usuario no tiene un archivo PDF asociado"));
            }
            
            String relativePath = user.pdfPath();
            Path pdfPath = null;
            
            File directFile = new File(relativePath);
            if (directFile.exists() && directFile.isFile()) {
                pdfPath = directFile.toPath();
            } else {
                Path rootRelativePath = Paths.get(".", relativePath);
                if (Files.exists(rootRelativePath)) {
                    pdfPath = rootRelativePath;
                } else {
                    Path baseRelativePath = Paths.get(pdfBaseDirectory).getParent().resolve(relativePath);
                    if (Files.exists(baseRelativePath)) {
                        pdfPath = baseRelativePath;
                    }
                }
            }
            
            if (pdfPath == null || !Files.exists(pdfPath)) {
                String fileName = Paths.get(relativePath).getFileName().toString();
                String userFolder = "user_" + userId;
                Path expectedPath = Paths.get(pdfBaseDirectory, userFolder, fileName);
                
                if (Files.exists(expectedPath)) {
                    pdfPath = expectedPath;
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "No se pudo acceder al archivo PDF"));
                }
            }
            
            try {
                Resource resource = new UrlResource(pdfPath.toUri());
                
                if (!resource.exists() || !resource.isReadable()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "No se pudo acceder al archivo PDF"));
                }
                
                String filename = "user_" + userId + "_document.pdf";
                
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                        .body(resource);
                
            } catch (MalformedURLException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Error al acceder al archivo: " + e.getMessage()));
            }
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Endpoint para eliminar el PDF de un usuario.
     * 
     * @param userId ID del usuario cuyo PDF se quiere eliminar
     * @param authHeader El token de autenticación
     * @return Respuesta con información del resultado
     */
    @DeleteMapping("/{userId}/pdf")
    public ResponseEntity<Map<String, Object>> deleteUserPdf(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "No se proporcionó un token de autenticación válido"));
        }

        String token = authHeader.substring(7);
        
        // Verificar autorización
        if (!isAuthorized(token, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "error", "No tienes permiso para eliminar el PDF de este usuario"));
        }
        
        try {
            // Obtener el usuario al que se le eliminará el PDF
            UserDTO targetUser = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("Usuario objetivo no encontrado"));
            
            if (targetUser.pdfPath() == null || targetUser.pdfPath().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "error", "El usuario no tiene un archivo PDF asociado"));
            }
            
            try {
                UserDTO updatedUser = userService.deleteUserPdf(targetUser);
                return ResponseEntity.ok(Map.of(
                    "success", true, 
                    "message", "Archivo PDF eliminado correctamente"
                ));
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "error", "Error al eliminar el archivo: " + e.getMessage()));
            }
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
} 