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
 */
@Component
public class SqlInjectionFilter extends OncePerRequestFilter {

    // Patrones sospechosos de inyección SQL
    private static final List<Pattern> SUSPICIOUS_PATTERNS = Arrays.asList(
            Pattern.compile(".*[';]+.*"),
            Pattern.compile(".*--.*"),
            Pattern.compile(".*\\bdrop\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\bdelete\\b.*\\bfrom\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\binsert\\b.*\\binto\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\bupdate\\b.*\\bset\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\bunion\\b.*\\bselect\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\bexec\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\bor\\b.*\\b1\\b\\s*=\\s*\\b1\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\bor\\b.*\\b'\\b\\s*=\\s*\\b'\\b.*", Pattern.CASE_INSENSITIVE)
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Rutas a excluir (recursos estáticos)
        String path = request.getRequestURI();
        if (path.matches(".*(css|jpg|png|gif|js|ico|woff|woff2)$")) {
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
            if (pattern.matcher(value).matches()) {
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
                "origin", "cache-control", "pragma", "if-modified-since", "if-none-match"
        );
        return standardHeaders.contains(headerName.toLowerCase());
    }
} 