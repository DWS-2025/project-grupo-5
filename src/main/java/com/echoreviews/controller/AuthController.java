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
    
    // Patrón para sanitizar entradas y prevenir inyecciones SQL
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
     * Método para sanitizar entradas de usuario y prevenir inyecciones SQL
     * @param input El texto a sanitizar
     * @return true si la entrada es segura, false en caso contrario
     */
    private boolean isSafeInput(String input) {
        return input != null && SAFE_INPUT_PATTERN.matcher(input).matches();
    }

    @PostMapping("/register")
    public String register(@ModelAttribute User user, RedirectAttributes redirectAttributes, Model model) {
        try {
            // Sanitizar entradas para prevenir inyecciones SQL
            String username = user.getUsername();
            String email = user.getEmail();
            
            // Verificar que el nombre de usuario sea seguro usando la utilidad de sanitización
            if (!inputSanitizer.isValidUsername(username)) {
                model.addAttribute("error", "El nombre de usuario contiene caracteres no permitidos");
                return "error";
            }
            
            // Verificar que el email sea seguro
            if (!inputSanitizer.isValidEmail(email)) {
                model.addAttribute("error", "El formato de email no es válido");
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