package com.sonidovivo.gateway.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * CORS centralizado del Gateway para el frontend React/Vite.
 *
 * Se usan origenes explicitos; no se permite "*" porque el Gateway es el
 * punto de entrada publico y podria incorporar credenciales en el futuro.
 * La lista se puede sobrescribir con la variable CORS_ALLOWED_ORIGINS.
 */
@Configuration
public class CorsConfig {

    private static final List<String> METODOS = List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    );

    private static final List<String> ENCABEZADOS = List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With",
            "Cache-Control",
            "Pragma"
    );

    @Bean
    public CorsWebFilter corsWebFilter(
            @Value("${gateway.cors.allowed-origins}") String origenesPermitidos
    ) {
        List<String> origenes = Arrays.stream(origenesPermitidos.split(","))
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .toList();

        if (origenes.isEmpty()) {
            throw new IllegalStateException("gateway.cors.allowed-origins no puede estar vacio");
        }

        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(origenes);
        configuracion.setAllowedMethods(METODOS);
        configuracion.setAllowedHeaders(ENCABEZADOS);
        configuracion.setExposedHeaders(List.of("Location"));

        // El frontend usa JWT en el header Authorization, no cookies de sesion.
        // Si en el futuro se usan cookies, cambiar a true manteniendo origenes explicitos.
        configuracion.setAllowCredentials(false);
        configuracion.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return new CorsWebFilter(fuente);
    }
}
