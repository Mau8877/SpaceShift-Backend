package com.sw.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    /**
     * CorsConfigurationSource bean usado directamente por Spring Security
     * (con Customizer.withDefaults() en SecurityConfig).
     * Esto evita el 403 que ocurre cuando Spring Security rechaza la petición
     * antes de evaluar las reglas permitAll() por no encontrar una configuración CORS válida.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Orígenes permitidos (frontend en desarrollo)
        config.setAllowedOriginPatterns(List.of("*"));

        // Métodos HTTP permitidos
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Headers permitidos en la petición
        config.setAllowedHeaders(List.of("*"));

        // Headers expuestos al cliente (necesario para leer Set-Cookie desde JS)
        config.setExposedHeaders(List.of("Set-Cookie", "Authorization"));

        // Permitir cookies y credenciales cross-origin
        config.setAllowCredentials(true);

        // Cache del preflight OPTIONS (en segundos)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}