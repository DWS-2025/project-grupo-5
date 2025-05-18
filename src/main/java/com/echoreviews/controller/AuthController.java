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

    // Web Routes
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
}