package com.echoreviews.controller;

import com.echoreviews.dto.UserDTO;
import com.echoreviews.model.User;
import com.echoreviews.security.JwtUtil;
import com.echoreviews.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
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

@Controller
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

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

    @PostMapping("/register")
    public String register(@ModelAttribute User user, RedirectAttributes redirectAttributes, Model model) {
        try {
            // Check if username already exists
            if (userService.getUserByUsername(user.getUsername()).isPresent()) {
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
                    user.getUsername(),
                    user.getPassword(),
                    user.getEmail(),
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
    }

    @PostMapping("/api/auth/register")
    @ResponseBody
    public ResponseEntity<?> apiRegister(@RequestBody UserDTO userDTO) {
        UserDTO registeredUser = userService.registerUser(userDTO);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Registration successful. Please login.");
        response.put("user", registeredUser);

        return ResponseEntity.ok(response);
    }
}