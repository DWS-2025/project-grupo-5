package com.echoreviews.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Utilidad para sanitizar entradas de usuario y prevenir inyecciones SQL
 * Esta clase proporciona métodos para validar y sanitizar diferentes tipos de entradas
 */
@Component
public class InputSanitizer {

    // Patrones de validación para diferentes tipos de entrada
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._@-]{3,50}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s._-]{2,50}$");
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^[0-9]+$");
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s]+$");

    /**
     * Valida si un nombre de usuario es seguro
     * @param username El nombre de usuario a validar
     * @return true si es válido, false en caso contrario
     */
    public boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * Valida si un email es seguro
     * @param email El email a validar
     * @return true si es válido, false en caso contrario
     */
    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Valida si un nombre es seguro
     * @param name El nombre a validar
     * @return true si es válido, false en caso contrario
     */
    public boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name).matches();
    }

    /**
     * Valida si un valor es numérico
     * @param value El valor a validar
     * @return true si es válido, false en caso contrario
     */
    public boolean isNumeric(String value) {
        return value != null && NUMERIC_PATTERN.matcher(value).matches();
    }

    /**
     * Sanitiza una entrada para prevenir XSS
     * @param input La entrada a sanitizar
     * @return La entrada sanitizada
     */
    public String sanitizeHtml(String input) {
        if (input == null) {
            return null;
        }
        
        // Reemplazar caracteres especiales HTML
        return input.replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&#x27;")
                .replaceAll("/", "&#x2F;");
    }

    /**
     * Sanitiza una entrada para prevenir inyecciones SQL básicas
     * @param input La entrada a sanitizar
     * @return La entrada sanitizada o null si la entrada es potencialmente peligrosa
     */
    public String sanitizeSql(String input) {
        if (input == null) {
            return null;
        }
        
        // Detectar patrones peligrosos
        if (input.contains("'") || 
            input.contains("\"") || 
            input.contains(";") || 
            input.contains("--") ||
            input.toLowerCase().contains(" or ") ||
            input.toLowerCase().contains(" union ") ||
            input.toLowerCase().contains(" select ") ||
            input.toLowerCase().contains(" insert ") ||
            input.toLowerCase().contains(" update ") ||
            input.toLowerCase().contains(" delete ") ||
            input.toLowerCase().contains(" drop ")) {
            
            return null; // Rechazar entradas potencialmente peligrosas
        }
        
        return input;
    }

    /**
     * Escapa caracteres especiales en SQL
     * @param input La entrada a escapar
     * @return La entrada con caracteres escapados
     * Nota: Este método NO debe utilizarse como única protección contra inyecciones SQL
     */
    public String escapeSql(String input) {
        if (input == null) {
            return null;
        }
        
        return input.replace("'", "''")
                .replace("\\", "\\\\");
    }
} 