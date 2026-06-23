package com.sw.api.security;

import org.springframework.http.HttpMethod;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                // 1. Desactivamos CSRF porque nosotros mismos manejaremos la seguridad con
                // nuestro JWT
                .csrf(csrf -> csrf.disable())

                // 2. Configuramos las reglas de acceso a las rutas (Endpoints)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Dejamos públicas las rutas de registro, login, la documentación de Swagger, error, webhook de Stripe y paquetes
                        .requestMatchers("/api/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/ws-chat/**", "/error", "/api/webhooks/stripe", "/api/tokens/paquetes", "/api/asistente/**")
                        .permitAll()
                        // Permitimos ver publicaciones, inmuebles y los modelos 3D (tour) sin estar logueado
                        .requestMatchers(HttpMethod.GET, "/api/publicaciones/**", "/api/inmuebles/**", "/api/videos/publicaciones/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/**").authenticated()
                        .requestMatchers("/api/usuarios/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/reportes/**").hasAuthority("ROLE_ADMIN")
                        // Cualquier otra petición a la API exigirá un token válido
                        .anyRequest().authenticated())

                // 3. Le decimos a Spring que NO guarde sesiones en memoria (Arquitectura
                // Stateless)
                // Esto es vital para APIs REST modernas y para que el JWT funcione bien en
                // móviles y web
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. Conectamos el proveedor que creamos hace un momento
                .authenticationProvider(authenticationProvider)

                // 5. Ponemos a nuestro "Guardián" (el filtro JWT) justo antes del filtro por
                // defecto de Spring
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}


