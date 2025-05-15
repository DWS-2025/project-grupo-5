package com.musicstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
public class SessionConfig {

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        // En entorno de desarrollo local, podemos deshabilitar esto si no usamos HTTPS
        // serializer.setUseSecureCookie(true);
        serializer.setUseSecureCookie(false); // Para desarrollo local sin HTTPS
        serializer.setSameSite("Lax"); // Menos restrictivo para facilitar el desarrollo
        serializer.setCookiePath("/"); // Establece el path de la cookie
        serializer.setCookieName("JSESSIONID"); // Usar el nombre estándar para compatibilidad
        serializer.setUseHttpOnlyCookie(true); // Previene acceso por JavaScript
        return serializer;
    }
} 