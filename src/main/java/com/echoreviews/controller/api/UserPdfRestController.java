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

        // Extraer el token
        String token = authHeader.substring(7);
        
        try {
            // Asegurar que el directorio base exista
            File directory = new File(pdfBaseDirectory);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            // Obtener el nombre de usuario del token
            String username = jwtUtil.extractUsername(token);
            
            // Obtener el usuario autenticado
            UserDTO currentUser = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Verificar que el usuario autenticado es el mismo al que se le asignará el PDF o es admin
            if (!currentUser.id().equals(userId) && !currentUser.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "error", "No tienes permiso para subir archivos PDF a este usuario"));
            }
            
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
        
        // Verificar que el token existe y tiene el formato correcto
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No se proporcionó un token de autenticación válido"));
        }

        // Extraer el token
        String token = authHeader.substring(7);
        
        try {
            // Obtener el usuario por ID
            Optional<UserDTO> userOpt = userService.getUserById(userId);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            UserDTO user = userOpt.get();
            
            // Verificar que el usuario tiene un PDF asociado
            if (user.pdfPath() == null || user.pdfPath().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "El usuario no tiene un archivo PDF asociado"));
            }
            
            // Resolver la ruta relativa a absoluta
            String relativePath = user.pdfPath();
            Path pdfPath = null;
            
            // Intentar diferentes estrategias para encontrar el archivo
            File directFile = new File(relativePath);
            if (directFile.exists() && directFile.isFile()) {
                pdfPath = directFile.toPath();
            } else {
                // Intentar con la ruta relativa desde la raíz del proyecto
                Path rootRelativePath = Paths.get(".", relativePath);
                if (Files.exists(rootRelativePath)) {
                    pdfPath = rootRelativePath;
                } else {
                    // Intentar con el directorio base configurado
                    Path baseRelativePath = Paths.get(pdfBaseDirectory).getParent().resolve(relativePath);
                    if (Files.exists(baseRelativePath)) {
                        pdfPath = baseRelativePath;
                    }
                }
            }
            
            // Si no se pudo encontrar el archivo, intentar buscar por nombre en el directorio esperable
            if (pdfPath == null || !Files.exists(pdfPath)) {
                String fileName = Paths.get(relativePath).getFileName().toString();
                String userFolder = "user_" + userId;
                Path expectedPath = Paths.get(pdfBaseDirectory, userFolder, fileName);
                
                if (Files.exists(expectedPath)) {
                    pdfPath = expectedPath;
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of(
                                "error", "No se pudo acceder al archivo PDF",
                                "path", relativePath,
                                "userFolder", userFolder,
                                "fileName", fileName,
                                "directorio", pdfBaseDirectory
                            ));
                }
            }
            
            // Aquí ya deberíamos tener la ruta correcta
            Resource resource;
            
            try {
                resource = new UrlResource(pdfPath.toUri());
                
                // Verificar que el archivo existe y es legible
                if (!resource.exists() || !resource.isReadable()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "No se pudo acceder al archivo PDF"));
                }
                
                // Construir el nombre de archivo para la descarga
                String filename = "user_" + userId + "_document.pdf";
                
                // Devolver el recurso como respuesta para descarga
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
        
        // Verificar que el token existe y tiene el formato correcto
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "No se proporcionó un token de autenticación válido"));
        }

        // Extraer el token
        String token = authHeader.substring(7);
        
        try {
            // Obtener el nombre de usuario del token
            String username = jwtUtil.extractUsername(token);
            
            // Obtener el usuario autenticado
            UserDTO currentUser = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Verificar que el usuario autenticado es el mismo al que se le eliminará el PDF o es admin
            if (!currentUser.id().equals(userId) && !currentUser.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "error", "No tienes permiso para eliminar el PDF de este usuario"));
            }
            
            // Obtener el usuario al que se le eliminará el PDF
            UserDTO targetUser = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("Usuario objetivo no encontrado"));
            
            // Verificar que el usuario tiene un PDF asociado
            if (targetUser.pdfPath() == null || targetUser.pdfPath().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "error", "El usuario no tiene un archivo PDF asociado"));
            }
            
            // Eliminar el PDF
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