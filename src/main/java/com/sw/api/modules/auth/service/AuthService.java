package com.sw.api.modules.auth.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.sw.api.modules.auth.dto.*;
import com.sw.api.shared.service.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sw.api.modules.usuario.enums.NombreRol;
import com.sw.api.modules.usuario.enums.NombreTipoPerfil;
import com.sw.api.modules.usuario.model.Perfil;
import com.sw.api.modules.usuario.model.Rol;
import com.sw.api.modules.usuario.model.TipoPerfil;
import com.sw.api.modules.usuario.model.Usuario;
import com.sw.api.modules.usuario.repository.PerfilRepository;
import com.sw.api.modules.usuario.repository.RolRepository;
import com.sw.api.modules.usuario.repository.TipoPerfilRepository;
import com.sw.api.modules.usuario.repository.UsuarioRepository;
import com.sw.api.security.JwtService;

import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;

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
    private final EmailService emailService;

    private static final SecureRandom random = new SecureRandom();

    @Transactional
    public AuthResponse registrar(RegisterRequest request) {
        if (usuarioRepository.findByCorreo(request.correo()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo ya estÃ¡ registrado");
        }

        NombreTipoPerfil nombreTipoPerfil;
        try {
            nombreTipoPerfil = NombreTipoPerfil.valueOf(request.tipoPerfil().trim().toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de perfil invalido");
        }

        TipoPerfil tipoSeleccionado = tipoPerfilRepository.findByNombre(nombreTipoPerfil)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de perfil invalido"));

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
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.correo(), request.password()));
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invÃ¡lido o corrupto");
        }

        if (correo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo extraer informaciÃ³n del token");
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

    @Transactional
    public MensajeResponse solicitarRecuperacion(RecuperarPasswordRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.correo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        String codigo = String.format("%06d", random.nextInt(1000000));
        LocalDateTime expiracion = LocalDateTime.now().plusMinutes(15);

        usuario.setCodigoRecuperacion(codigo);
        usuario.setExpiracionCodigoRecuperacion(expiracion);
        usuarioRepository.save(usuario);

        emailService.enviarCodigoRecuperacion(usuario.getCorreo(), codigo);

        return new MensajeResponse("Código de recuperación enviado al correo");
    }

    @Transactional
    public MensajeResponse validarCodigo(ValidarCodigoRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.correo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (usuario.getCodigoRecuperacion() == null || usuario.getExpiracionCodigoRecuperacion() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se ha solicitado un código de recuperación");
        }

        if (LocalDateTime.now().isAfter(usuario.getExpiracionCodigoRecuperacion())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código ha expirado");
        }

        if (!usuario.getCodigoRecuperacion().equals(request.codigo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido");
        }

        return new MensajeResponse("Código válido");
    }

    @Transactional
    public MensajeResponse cambiarPassword(CambiarPasswordRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.correo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (usuario.getCodigoRecuperacion() == null || usuario.getExpiracionCodigoRecuperacion() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se ha solicitado un código de recuperación");
        }

        if (LocalDateTime.now().isAfter(usuario.getExpiracionCodigoRecuperacion())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código ha expirado");
        }

        if (!usuario.getCodigoRecuperacion().equals(request.codigo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido");
        }

        usuario.setPassword(passwordEncoder.encode(request.nuevaPassword()));
        usuario.setCodigoRecuperacion(null);
        usuario.setExpiracionCodigoRecuperacion(null);
        usuarioRepository.save(usuario);

        return new MensajeResponse("Contraseña actualizada exitosamente");
    }
}

