package com.echoreviews.controller.api;

import com.echoreviews.security.JwtUtil;
import com.echoreviews.service.UserService;
import com.echoreviews.dto.UserDTO;
import com.echoreviews.util.InputSanitizer;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private InputSanitizer inputSanitizer;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");
        
        // Sanitize inputs to prevent SQL injections using the sanitization utility
        if (!inputSanitizer.isValidUsername(username)) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid username format");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            UserDTO user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String jwt = jwtUtil.generateToken(userDetails, user.isAdmin());

            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("user", user);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Authentication failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        // Verify that the token exists and has the correct format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Token not provided or invalid format");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        // Extract the token
        String token = authHeader.substring(7);
        
        try {
            // Invalidate the token
            jwtUtil.invalidateToken(token);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Logout successful. Token invalidated.");
            
            return ResponseEntity.ok(response);
        } catch (ExpiredJwtException e) {
            // If the token is already expired, consider the logout successful
            Map<String, String> response = new HashMap<>();
            response.put("message", "Logout successful. Token already expired.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // Format error or invalid token
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid token: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            // Any other error
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Logout failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDTO userDTO) {
        // Sanitize inputs to prevent SQL injections using the sanitization utility
        if (!inputSanitizer.isValidUsername(userDTO.username())) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid username format");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // Verify that the email is safe
        if (!inputSanitizer.isValidEmail(userDTO.email())) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid email format");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        try {
            UserDTO safeUserDTO = userDTO.withPdfPath(null).withImageUrl(null);
            UserDTO registeredUser = userService.registerUser(safeUserDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Registration successful. Please login.");
            response.put("user", registeredUser);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Registration failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
} 