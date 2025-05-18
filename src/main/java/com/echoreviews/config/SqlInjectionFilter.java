package com.echoreviews.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Filtro para prevenir inyecciones SQL
 * Este filtro inspecciona los parámetros de la solicitud y los encabezados
 * en busca de patrones sospechosos de inyección SQL
 * 
 * Nota: Temporalmente deshabilitado para resolver problemas con CSS.
 */
// @Component // Comentado temporalmente para desactivar el filtro
public class SqlInjectionFilter extends OncePerRequestFilter {

    // Patrones sospechosos de inyección SQL
    private static final List<Pattern> SUSPICIOUS_PATTERNS = Arrays.asList(
            // Buscar comillas simples solo si van seguidas de comandos SQL o está en un contexto sospechoso
            Pattern.compile(".*'\\s*(or|and|insert|update|delete|drop|alter|select|union)\\s+.*", Pattern.CASE_INSENSITIVE),
            // Detectar comentarios SQL seguidos de comandos
            Pattern.compile(".*--\\s*.*", Pattern.CASE_INSENSITIVE),
            // Comandos destructivos más específicos
            Pattern.compile(".*\\bdrop\\s+table\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\bdelete\\s+from\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\binsert\\s+into\\b.*\\bvalues\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\bupdate\\s+\\w+\\s+set\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\bunion\\s+select\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\bexec\\s+\\w+\\b.*", Pattern.CASE_INSENSITIVE),
            // Detectar casos clásicos de bypass de autenticación
            Pattern.compile(".*\\bor\\s+1\\s*=\\s*1\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\bor\\s+'\\s*'\\s*=\\s*'\\s*'\\b.*", Pattern.CASE_INSENSITIVE)
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Rutas a excluir (recursos estáticos)
        String path = request.getRequestURI();
        
        // Mejorar la exclusión de recursos estáticos
        if (path.contains("/css/") || 
            path.contains("/js/") || 
            path.contains("/images/") || 
            path.contains("/webjars/") ||
            path.contains("/fonts/") ||
            path.endsWith(".css") || 
            path.endsWith(".js") || 
            path.endsWith(".jpg") || 
            path.endsWith(".jpeg") || 
            path.endsWith(".png") || 
            path.endsWith(".gif") || 
            path.endsWith(".ico") || 
            path.endsWith(".woff") || 
            path.endsWith(".woff2") || 
            path.endsWith(".ttf") || 
            path.endsWith(".svg")) {
            
            filterChain.doFilter(request, response);
            return;
        }

        // Comprobar parámetros de la solicitud
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String[] paramValues = request.getParameterValues(paramName);
            if (paramValues != null) {
                for (String paramValue : paramValues) {
                    if (isSqlInjectionSuspicious(paramValue)) {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Posible intento de inyección SQL detectado.");
                        return;
                    }
                }
            }
        }

        // Comprobar valores en los encabezados personalizados
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            // Excluir cabeceras estándar que podrían contener contenido complejo
            if (!isStandardHeader(headerName)) {
                String headerValue = request.getHeader(headerName);
                if (isSqlInjectionSuspicious(headerValue)) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Posible intento de inyección SQL detectado.");
                    return;
                }
            }
        }

        // Si no se detecta inyección, continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }

    /**
     * Comprueba si una cadena contiene patrones sospechosos de inyección SQL
     * @param value Valor a comprobar
     * @return true si es sospechoso, false si no
     */
    private boolean isSqlInjectionSuspicious(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        // Comprobar contra patrones sospechosos
        for (Pattern pattern : SUSPICIOUS_PATTERNS) {
            if (pattern.matcher(value).find()) {
                // Registrar el patrón que coincidió y el valor para depuración
                System.out.println("Posible inyección SQL detectada: " + value);
                System.out.println("Patrón que coincidió: " + pattern.pattern());
                return true;
            }
        }

        return false;
    }

    /**
     * Determina si un nombre de encabezado es estándar o no
     * Los encabezados estándar no se comprueban para inyección SQL
     * @param headerName Nombre del encabezado
     * @return true si es un encabezado estándar, false en caso contrario
     */
    private boolean isStandardHeader(String headerName) {
        List<String> standardHeaders = Arrays.asList(
                "host", "user-agent", "accept", "accept-language", "accept-encoding",
                "connection", "referer", "cookie", "content-length", "content-type",
                "origin", "cache-control", "pragma", "if-modified-since", "if-none-match",
                "x-requested-with", "x-forwarded-for", "x-forwarded-proto", "x-csrf-token",
                "authorization", "sec-fetch-dest", "sec-fetch-mode", "sec-fetch-site", 
                "sec-fetch-user", "upgrade-insecure-requests", "x-real-ip", "sec-ch-ua",
                "sec-ch-ua-mobile", "sec-ch-ua-platform", "access-control-request-method",
                "access-control-request-headers", "dnt", "date", "via", "x-xss-protection",
                "x-content-type-options"
        );
        return standardHeaders.contains(headerName.toLowerCase());
    }
} 