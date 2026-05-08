package com.sw.api.modules.auth.service;

import com.sw.api.modules.auth.dto.AuthResponse;
import com.sw.api.modules.auth.dto.LoginRequest;
import com.sw.api.modules.auth.dto.RegisterRequest;
import com.sw.api.modules.auth.dto.RefreshTokenRequest;
import com.sw.api.modules.usuario.model.Usuario;
import com.sw.api.modules.usuario.model.Perfil;
import com.sw.api.modules.usuario.model.TipoPerfil;
import com.sw.api.modules.usuario.model.Rol;
import com.sw.api.modules.usuario.repository.UsuarioRepository;
import com.sw.api.modules.usuario.repository.PerfilRepository;
import com.sw.api.modules.usuario.repository.TipoPerfilRepository;
import com.sw.api.modules.usuario.model.NombreRol;
import com.sw.api.modules.usuario.repository.RolRepository;
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

import io.jsonwebtoken.ExpiredJwtException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PerfilRepository perfilRepository;
    private final TipoPerfilRepository tipoPerfilRepository;
    private final RolRepository rolRepository;

    @Transactional
    public AuthResponse registrar(RegisterRequest request) {
        if (usuarioRepository.findByCorreo(request.correo()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo ya está registrado");
        }

        TipoPerfil tipoSeleccionado = tipoPerfilRepository.findByNombre(request.tipoPerfil())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de perfil inválido"));

        Rol rolPorDefecto = rolRepository.findByNombre(NombreRol.ROLE_USER.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Rol base no encontrado en la DB"));

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setCorreo(request.correo());
        nuevoUsuario.setPassword(passwordEncoder.encode(request.password()));
        nuevoUsuario.setEstadoConexion(true);
        nuevoUsuario.setUltimaConexion(LocalDateTime.now());
        nuevoUsuario.setRol(rolPorDefecto);

        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);

        Perfil nuevoPerfil = new Perfil();
        nuevoPerfil.setNombre(request.nombre());
        nuevoPerfil.setApellido(request.apellido());
        nuevoPerfil.setFotoUrl(request.fotoUrl());
        nuevoPerfil.setUsuario(usuarioGuardado);
        nuevoPerfil.setTipoPerfil(tipoSeleccionado);

        perfilRepository.save(nuevoPerfil);

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("id", usuarioGuardado.getId());
        extraClaims.put("nombre", nuevoPerfil.getNombre());
        extraClaims.put("apellido", nuevoPerfil.getApellido());
        extraClaims.put("rol", usuarioGuardado.getRol().getNombre());

        var jwtToken = jwtService.generarToken(extraClaims, usuarioGuardado);
        return new AuthResponse(jwtToken);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.correo(), request.password()));

        var user = usuarioRepository.findByCorreo(request.correo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        var perfil = perfilRepository.findByUsuario(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("id", user.getId());
        extraClaims.put("nombre", perfil.getNombre());
        extraClaims.put("apellido", perfil.getApellido());
        String nombreRol = (user.getRol() != null) ? user.getRol().getNombre() : "SIN_ROL";
        extraClaims.put("rol", nombreRol);

        var jwtToken = jwtService.generarToken(extraClaims, user);
        return new AuthResponse(jwtToken);
    }

    public AuthResponse actualizarToken(RefreshTokenRequest request) {
        String tokenViejo = request.token();
        String correo = null;

        try {
            correo = jwtService.extractUsername(tokenViejo);
        } catch (ExpiredJwtException e) {
            correo = e.getClaims().getSubject();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido o corrupto");
        }

        if (correo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo extraer información del token");
        }

        var user = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuario no encontrado en la base de datos"));

        var perfil = perfilRepository.findByUsuario(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("id", user.getId());
        extraClaims.put("nombre", perfil.getNombre());
        extraClaims.put("apellido", perfil.getApellido());
        String nombreRol = (user.getRol() != null) ? user.getRol().getNombre() : "SIN_ROL";
        extraClaims.put("rol", nombreRol);

        var nuevoToken = jwtService.generarToken(extraClaims, user);
        return new AuthResponse(nuevoToken);
    }
}
