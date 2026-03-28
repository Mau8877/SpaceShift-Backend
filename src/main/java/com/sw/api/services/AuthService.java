package com.sw.api.services;

import com.sw.api.dtos.AuthResponse;
import com.sw.api.dtos.LoginRequest;
import com.sw.api.dtos.RegisterRequest;
import com.sw.api.models.Usuario;
import com.sw.api.models.Perfil;
import com.sw.api.models.TipoPerfil;
import com.sw.api.models.Rol;
import com.sw.api.repositories.UsuarioRepository;
import com.sw.api.repositories.PerfilRepository;
import com.sw.api.repositories.TipoPerfilRepository;
import com.sw.api.repositories.RolRepository;
import com.sw.api.security.JwtService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PerfilRepository perfilRepository;
    private final TipoPerfilRepository tipoPerfilRepository;
    private final RolRepository rolRepository; // Inyectamos el nuevo repositorio

    @Transactional
    public AuthResponse registrar(RegisterRequest request) {
        // 1. Validar si el correo existe (ahora responde con un error 400 limpio)
        if (usuarioRepository.findByCorreo(request.correo()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo ya está registrado");
        }

        // 2. Buscar catálogos (Tipo de Perfil y Rol)
        TipoPerfil tipoSeleccionado = tipoPerfilRepository.findByNombre(request.tipoPerfil())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de perfil inválido"));

        Rol rolPorDefecto = rolRepository.findByNombre("ROLE_USER")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Rol base no encontrado en la DB"));

        // 3. Crear y guardar Usuario
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setCorreo(request.correo());
        nuevoUsuario.setPassword(passwordEncoder.encode(request.password()));
        nuevoUsuario.setEstadoConexion(true);
        nuevoUsuario.setUltimaConexion(LocalDateTime.now());
        nuevoUsuario.setRol(rolPorDefecto); // ¡Asignamos el rol aquí!

        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);

        // 4. Crear y guardar Perfil
        Perfil nuevoPerfil = new Perfil();
        nuevoPerfil.setNombre(request.nombre());
        nuevoPerfil.setApellido(request.apellido());
        nuevoPerfil.setFotoUrl(request.fotoUrl());
        nuevoPerfil.setUsuario(usuarioGuardado);
        nuevoPerfil.setTipoPerfil(tipoSeleccionado);

        perfilRepository.save(nuevoPerfil);

        // 5. Armar los Custom Claims para el JWT 
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("id", usuarioGuardado.getId());
        extraClaims.put("nombre", nuevoPerfil.getNombre());
        extraClaims.put("apellido", nuevoPerfil.getApellido());
        extraClaims.put("rol", usuarioGuardado.getRol().getNombre());

        // 6. Generar token
        var jwtToken = jwtService.generarToken(extraClaims, usuarioGuardado);
        return new AuthResponse(jwtToken);
    }

    public AuthResponse login(LoginRequest request) {
        // 1. Verificar credenciales
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.correo(), request.password())
        );

        // 2. Buscar Usuario
        var user = usuarioRepository.findByCorreo(request.correo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // 3. Buscar Perfil
        var perfil = perfilRepository.findByUsuario(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));

        // 4. Armar los Custom Claims para el JWT 
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("id", user.getId());
        extraClaims.put("nombre", perfil.getNombre());
        extraClaims.put("apellido", perfil.getApellido());
        
        // 5. Generar token
        var jwtToken = jwtService.generarToken(extraClaims, user);
        return new AuthResponse(jwtToken);
    }
}