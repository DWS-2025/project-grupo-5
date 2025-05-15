package com.musicstore.security;

import com.musicstore.dto.UserDTO;
import com.musicstore.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final UserService userService;

    public CustomAuthenticationSuccessHandler(UserService userService) {
        this.userService = userService;
        setDefaultTargetUrl("/"); // Default redirect target
        setAlwaysUseDefaultTargetUrl(false); // Use saved request if available
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        String username;
        if (authentication.getPrincipal() instanceof UserDetails) {
            username = ((UserDetails) authentication.getPrincipal()).getUsername();
        } else {
            username = authentication.getPrincipal().toString();
        }

        UserDTO userDTO = userService.getUserByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User " + username + " authenticated but not found in database."));

        HttpSession session = request.getSession(true); // Garantizar que la sesión existe
        
        // Almacenar información del usuario en la sesión
        session.setAttribute("user", userDTO);
        
        // Generar un token de autenticación aleatorio
        String authToken = UUID.randomUUID().toString();
        session.setAttribute("authToken", authToken);
        
        // Almacenar información de roles
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        session.setAttribute("isAdmin", isAdmin);
        
        System.out.println("[CustomAuthenticationSuccessHandler] UserDTO for '" + username + "' set in HttpSession.");
        System.out.println("[CustomAuthenticationSuccessHandler] User roles set - isAdmin: " + isAdmin);
        System.out.println("[CustomAuthenticationSuccessHandler] Auth token generated: " + authToken);

        super.onAuthenticationSuccess(request, response, authentication);
    }
} 