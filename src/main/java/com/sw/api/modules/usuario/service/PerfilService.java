package com.sw.api.modules.usuario.service;

import com.sw.api.modules.usuario.dto.PerfilPatchRequestDTO;
import com.sw.api.modules.usuario.dto.PerfilResponseDTO;
import com.sw.api.modules.usuario.model.Perfil;
import com.sw.api.modules.usuario.model.TipoPerfil;
import com.sw.api.modules.usuario.model.Usuario;
import com.sw.api.modules.usuario.repository.PerfilRepository;
import com.sw.api.modules.usuario.repository.TipoPerfilRepository;
import com.sw.api.modules.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PerfilService {

    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final TipoPerfilRepository tipoPerfilRepository;

    public PerfilResponseDTO obtenerPerfilPorCorreo(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Perfil perfil = perfilRepository.findByUsuario(usuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));

        return new PerfilResponseDTO(
                usuario.getCorreo(),
                usuario.isEstadoConexion(),
                perfil.getTipoPerfil().getNombre(),
                perfil.getNombre(),
                perfil.getApellido(),
                perfil.getFotoUrl());
    }

    public PerfilResponseDTO obtenerPerfilPorIdUsuario(UUID idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Perfil perfil = perfilRepository.findByUsuario(usuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));

        return new PerfilResponseDTO(
                usuario.getCorreo(),
                usuario.isEstadoConexion(),
                perfil.getTipoPerfil().getNombre(),
                perfil.getNombre(),
                perfil.getApellido(),
                perfil.getFotoUrl());
    }

    @Transactional
    public PerfilResponseDTO actualizarMiPerfil(UUID idUsuario, String correoAutenticado, PerfilPatchRequestDTO request) {
        Usuario usuarioAutenticado = usuarioRepository.findByCorreo(correoAutenticado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario autenticado no encontrado"));

        if (!usuarioAutenticado.getId().equals(idUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes editar tu propio perfil");
        }

        Perfil perfil = perfilRepository.findByUsuario(usuarioAutenticado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));

        if (request.correo() != null && !request.correo().isBlank()
                && !request.correo().equalsIgnoreCase(usuarioAutenticado.getCorreo())) {
            usuarioRepository.findByCorreo(request.correo())
                    .filter(existente -> !existente.getId().equals(usuarioAutenticado.getId()))
                    .ifPresent(existente -> {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo ya está registrado");
                    });
            usuarioAutenticado.setCorreo(request.correo());
        }

        if (request.estadoConexion() != null) {
            usuarioAutenticado.setEstadoConexion(request.estadoConexion());
        }

        if (request.nombre() != null) {
            perfil.setNombre(request.nombre());
        }

        if (request.apellido() != null) {
            perfil.setApellido(request.apellido());
        }

        if (request.fotoUrl() != null) {
            perfil.setFotoUrl(request.fotoUrl());
        }

        if (request.tipoPerfil() != null && !request.tipoPerfil().isBlank()) {
            TipoPerfil tipoPerfil = tipoPerfilRepository.findByNombre(request.tipoPerfil())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de perfil inválido"));
            perfil.setTipoPerfil(tipoPerfil);
        }

        usuarioRepository.save(usuarioAutenticado);
        perfilRepository.save(perfil);

        return new PerfilResponseDTO(
                usuarioAutenticado.getCorreo(),
                usuarioAutenticado.isEstadoConexion(),
                perfil.getTipoPerfil().getNombre(),
                perfil.getNombre(),
                perfil.getApellido(),
                perfil.getFotoUrl());
    }
}
