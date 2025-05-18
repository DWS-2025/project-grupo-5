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
    
    // Base directory for PDFs (default user.dir to use project directory)
    @Value("${app.pdf.storage.directory:./user-pdfs}")
    private String pdfBaseDirectory;
    
    private boolean isAuthorized(String token, Long userId) {
        try {
            String username = jwtUtil.extractUsername(token);
            UserDTO requestingUser = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // User is authorized if they are admin or the resource owner
            return requestingUser.isAdmin() || requestingUser.id().equals(userId);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Endpoint to upload a PDF file to a user.
     * User must be authenticated and can only upload PDF to their own profile.
     * 
     * @param userId ID of the user to assign the PDF to
     * @param pdf The PDF file to upload
     * @param authHeader The authentication token
     * @return Response with result information
     */
    @PostMapping("/{userId}/pdf")
    public ResponseEntity<Map<String, Object>> uploadUserPdf(
            @PathVariable Long userId,
            @RequestParam("pdf") MultipartFile pdf,
            @RequestHeader("Authorization") String authHeader) {
        
        // Verify that the token exists and has the correct format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "No valid authentication token provided"));
        }

        String token = authHeader.substring(7);
        
        // Verify authorization
        if (!isAuthorized(token, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "error", "You don't have permission to upload PDF files to this user"));
        }
        
        try {
            // Get the target user for the PDF
            UserDTO targetUser = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));
            
            // Upload the PDF
            PdfUploadResult result = userService.uploadUserPdf(targetUser, pdf);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                    "success", true, 
                    "message", "PDF file uploaded successfully",
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
     * Endpoint to download a user's PDF.
     * 
     * @param userId ID of the user whose PDF is to be downloaded
     * @param authHeader The authentication token
     * @return The PDF file for download
     */
    @GetMapping("/{userId}/pdf")
    public ResponseEntity<?> downloadUserPdf(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No valid authentication token provided"));
        }

        String token = authHeader.substring(7);
        
        // Verify authorization
        if (!isAuthorized(token, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have permission to download this user's PDF"));
        }
        
        try {
            Optional<UserDTO> userOpt = userService.getUserById(userId);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            UserDTO user = userOpt.get();
            
            if (user.pdfPath() == null || user.pdfPath().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User does not have an associated PDF file"));
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
                            .body(Map.of("error", "Could not access the PDF file"));
                }
            }
            
            try {
                Resource resource = new UrlResource(pdfPath.toUri());
                
                if (!resource.exists() || !resource.isReadable()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Could not access the PDF file"));
                }
                
                String filename = "user_" + userId + "_document.pdf";
                
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                        .body(resource);
                
            } catch (MalformedURLException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Error accessing file: " + e.getMessage()));
            }
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Endpoint to delete a user's PDF.
     * 
     * @param userId ID of the user whose PDF is to be deleted
     * @param authHeader The authentication token
     * @return Response with result information
     */
    @DeleteMapping("/{userId}/pdf")
    public ResponseEntity<Map<String, Object>> deleteUserPdf(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "No valid authentication token provided"));
        }

        String token = authHeader.substring(7);
        
        // Verify authorization
        if (!isAuthorized(token, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "error", "You don't have permission to delete this user's PDF"));
        }
        
        try {
            // Get the target user for the PDF
            UserDTO targetUser = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));
            
            if (targetUser.pdfPath() == null || targetUser.pdfPath().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "error", "User does not have an associated PDF file"));
            }
            
            try {
                UserDTO updatedUser = userService.deleteUserPdf(targetUser);
                return ResponseEntity.ok(Map.of(
                    "success", true, 
                    "message", "PDF file deleted successfully"
                ));
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "error", "Error deleting file: " + e.getMessage()));
            }
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
} 