package com.sw.api.services;

import com.sw.api.dtos.AuthResponse;
import com.sw.api.dtos.LoginRequest;
import com.sw.api.dtos.RegisterRequest;
import com.sw.api.models.Usuario;
import com.sw.api.repositories.UsuarioRepository;
import com.sw.api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse registrar(RegisterRequest request) {
        // 1. Creamos el usuario y encriptamos su contraseña
        Usuario user = new Usuario();
        user.setCorreo(request.correo());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEstadoConexion(false);
        // Nota: ¡No seteamos campos de auditoría porque Spring lo hará solo!

        // 2. Guardamos en PostgreSQL
        usuarioRepository.save(user);

        // 3. Generamos su Token JWT
        var jwtToken = jwtService.generarToken(user);
        return new AuthResponse(jwtToken);
    }

    public AuthResponse login(LoginRequest request) {
        // 1. Spring Security verifica si el correo y la contraseña (hasheada) coinciden
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.correo(), request.password())
        );

        // 2. Si pasamos el paso anterior, buscamos al usuario
        var user = usuarioRepository.findByCorreo(request.correo()).orElseThrow();

        // 3. Generamos un nuevo Token JWT
        var jwtToken = jwtService.generarToken(user);
        return new AuthResponse(jwtToken);
    }
}