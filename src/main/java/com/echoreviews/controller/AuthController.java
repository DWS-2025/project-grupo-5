package com.echoreviews.controller;

import com.echoreviews.dto.UserDTO;
import com.echoreviews.model.User;
import com.echoreviews.security.JwtUtil;
import com.echoreviews.service.UserService;
import com.echoreviews.util.InputSanitizer;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import io.jsonwebtoken.ExpiredJwtException;

@Controller
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private InputSanitizer inputSanitizer;
    
    // Pattern to sanitize inputs and prevent SQL injections
    private static final Pattern SAFE_INPUT_PATTERN = Pattern.compile("^[a-zA-Z0-9._@-]{3,50}$");

    // Rutas Web
    @GetMapping("/login")
    public String loginForm(Model model) {
        model.addAttribute("user", new User());
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }
    
    /**
     * Method to sanitize user inputs and prevent SQL injections
     * @param input The text to sanitize
     * @return true if the input is safe, false otherwise
     */
    private boolean isSafeInput(String input) {
        return input != null && SAFE_INPUT_PATTERN.matcher(input).matches();
    }

    @PostMapping("/register")
    public String register(@ModelAttribute User user, RedirectAttributes redirectAttributes, Model model) {
        try {
            // Sanitize inputs to prevent SQL injections
            String username = user.getUsername();
            String email = user.getEmail();
            
            // Verify that the username is safe using the sanitization utility
            if (!inputSanitizer.isValidUsername(username)) {
                model.addAttribute("error", "The username contains invalid characters");
                return "error";
            }
            
            // Verify that the email is safe
            if (!inputSanitizer.isValidEmail(email)) {
                model.addAttribute("error", "Invalid email format");
                return "error";
            }
            
            // Check if username already exists
            if (userService.getUserByUsername(username).isPresent()) {
                model.addAttribute("error", "Username already in use");
                return "error";
            }

            // Validate password
            String password = user.getPassword();
            if (!password.matches("^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#$%^&*(),.?\":{}|<>])(?=\\S+$).{8,25}$")) {
                model.addAttribute("error", "Password must be between 8 and 25 characters and contain at least one number, one uppercase letter, and one special character");
                return "error";
            }

            // Convert User to UserDTO
            UserDTO userDTO = new UserDTO(
                    null,
                    username,
                    password,
                    email,
                    false,
                    false,
                    false,
                    null,
                    null,
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>()
            );

            userService.registerUser(userDTO);
            redirectAttributes.addFlashAttribute("success", "Registration successful. Please login.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error_message", e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // Rutas API REST
    @PostMapping("/api/auth/login")
    @ResponseBody
    public ResponseEntity<?> apiLogin(@RequestBody Map<String, String> loginRequest) {
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

    @PostMapping("/api/auth/logout")
    @ResponseBody
    public ResponseEntity<?> apiLogout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
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

    @PostMapping("/api/auth/register")
    @ResponseBody
    public ResponseEntity<?> apiRegister(@RequestBody UserDTO userDTO) {
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
            UserDTO registeredUser = userService.registerUser(userDTO);
            
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