package com.musicstore.config;

import com.musicstore.dto.UserDTO;
import com.musicstore.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Manejador personalizado para acciones tras una autenticación exitosa.
 * Su función principal es obtener el UserDTO completo del usuario autenticado
 * y guardarlo en la HttpSession para que esté disponible en toda la aplicación,
 * manteniendo la compatibilidad con partes del código que dependen de este atributo de sesión.
 */
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;

    @Autowired
    public CustomAuthenticationSuccessHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String username;
        // Obtiene el nombre de usuario del objeto Principal de Spring Security
        if (authentication.getPrincipal() instanceof UserDetails) {
            username = ((UserDetails) authentication.getPrincipal()).getUsername();
        } else {
            username = authentication.getPrincipal().toString();
        }

        // Carga el UserDTO completo usando el servicio de usuario
        UserDTO userDTO = userService.getUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado durante la configuración de sesión tras el login: " + username));

        // Guarda el UserDTO en la sesión HTTP
        HttpSession session = request.getSession();
        session.setAttribute("user", userDTO);

        // Redirige al usuario a la página de inicio tras un login exitoso.
        // Spring Security podría manejar redirecciones más complejas (a la URL original solicitada)
        // si se usara SavedRequestAwareAuthenticationSuccessHandler, pero para este caso,
        // una redirección a la raíz es suficiente y consistente con la configuración de defaultSuccessUrl previa.
        response.sendRedirect(request.getContextPath() + "/");
    }
} 