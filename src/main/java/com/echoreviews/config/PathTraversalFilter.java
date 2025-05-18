package com.echoreviews.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Filtro para detectar y prevenir ataques de path traversal
 * Verifica patrones sospechosos en las URLs solicitadas
 */
@Component
public class PathTraversalFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(PathTraversalFilter.class);
    
    // Patrones de path traversal para detectar
    private static final Pattern SUSPICIOUS_PATH_PATTERN = Pattern.compile(
            "\\.\\./|\\.\\.\\\\|/\\.\\./|\\\\\\.\\.\\\\|%2e%2e%2f|%252e%252e%252f|%c0%ae%c0%ae%c0%af",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullPath = requestURI + (queryString != null ? "?" + queryString : "");
        
        // Log de todas las peticiones para depuración
        logger.info("Petición recibida: {} {}", request.getMethod(), fullPath);
        
        // Imprime la URL decodificada para ayudar a depurar
        String decodedUrl = java.net.URLDecoder.decode(fullPath, "UTF-8");
        logger.info("URL decodificada: {}", decodedUrl);
        
        // Verificación más simple para detectar patrones básicos de path traversal
        boolean containsPathTraversal = fullPath.contains("../") || 
                                        fullPath.contains("..\\") || 
                                        fullPath.contains("%2e%2e%2f") ||
                                        fullPath.contains("%2e%2e/") ||
                                        fullPath.contains("..%2f") ||
                                        decodedUrl.contains("../") ||
                                        decodedUrl.contains("..\\");
        
        // Verificar si hay patrón sospechoso de path traversal
        if (containsPathTraversal || SUSPICIOUS_PATH_PATTERN.matcher(fullPath).find() || SUSPICIOUS_PATH_PATTERN.matcher(decodedUrl).find()) {
            System.out.println("¡ALERTA! POSIBLE INTENTO DE PATH TRAVERSAL DETECTADO: " + fullPath);
            logger.error("POSIBLE INTENTO DE PATH TRAVERSAL DETECTADO:");
            logger.error("IP: {}", request.getRemoteAddr());
            logger.error("Método: {}", request.getMethod());
            logger.error("Ruta completa: {}", fullPath);
            logger.error("User-Agent: {}", request.getHeader("User-Agent"));
            
            // Devolver error 400 Bad Request
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Solicitud no válida: posible path traversal");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
} 