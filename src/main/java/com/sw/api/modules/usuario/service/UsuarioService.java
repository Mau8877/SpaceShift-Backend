package com.sw.api.modules.usuario.service;

import com.sw.api.modules.publicacion.repository.PublicacionRepository;
import com.sw.api.modules.usuario.dto.UsuarioListItemDTO;
import com.sw.api.modules.usuario.dto.UsuarioPageResponseDTO;
import com.sw.api.modules.usuario.dto.UsuarioPatchRequestDTO;
import com.sw.api.modules.usuario.dto.UsuarioRequestDTO;
import com.sw.api.modules.usuario.dto.UsuarioResponseDTO;
import com.sw.api.modules.usuario.dto.UsuarioStatsDTO;
import com.sw.api.modules.usuario.enums.NombreTipoPerfil;
import com.sw.api.modules.usuario.model.Perfil;
import com.sw.api.modules.usuario.model.Rol;
import com.sw.api.modules.usuario.model.TipoPerfil;
import com.sw.api.modules.usuario.model.Usuario;
import com.sw.api.modules.usuario.repository.PerfilRepository;
import com.sw.api.modules.usuario.repository.RolRepository;
import com.sw.api.modules.usuario.repository.TipoPerfilRepository;
import com.sw.api.modules.usuario.repository.UsuarioRepository;
import com.sw.api.modules.token.model.TipoTransaccion;
import com.sw.api.modules.token.model.TransaccionCredito;
import com.sw.api.modules.token.repository.TransaccionCreditoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final RolRepository rolRepository;
    private final TipoPerfilRepository tipoPerfilRepository;
    private final PublicacionRepository publicacionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransaccionCreditoRepository transaccionCreditoRepository;

    @Transactional(readOnly = true)
    public UsuarioPageResponseDTO listarUsuarios(int page, int size, String search, Boolean estado, Boolean estadoConexion) {
        int pageSafe = Math.max(page, 0);
        int sizeSafe = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(pageSafe, sizeSafe);

        Page<UsuarioRepository.UsuarioListProjection> result = usuarioRepository.findUsuariosAdmin(search, estado, estadoConexion,
                pageable);

        List<UsuarioListItemDTO> content = result.getContent().stream()
                .map(row -> new UsuarioListItemDTO(
                        row.getId(),
                        row.getCorreo(),
                        row.getNombre(),
                        row.getApellido(),
                        row.getTelefono(),
                        Boolean.TRUE.equals(row.getEstado()),
                        Boolean.TRUE.equals(row.getEstadoConexion()),
                        row.getRol(),
                        row.getTipoPerfil(),
                        valueOrZero(row.getTotalPublicaciones())))
                .toList();

        return new UsuarioPageResponseDTO(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                construirStats());
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO obtenerDetalle(UUID id) {
        UsuarioRepository.UsuarioDetailProjection detail = usuarioRepository.findDetalleAdminById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Long totalPublicaciones = publicacionRepository.countPublicacionesActivasByUsuarioId(id);

        return new UsuarioResponseDTO(
                detail.getId(),
                detail.getCorreo(),
                Boolean.TRUE.equals(detail.getEstado()),
                Boolean.TRUE.equals(detail.getEstadoConexion()),
                detail.getUltimaConexion(),
                detail.getRol(),
                detail.getCreatedDate(),
                detail.getLastModifiedDate(),
                detail.getNombre(),
                detail.getApellido(),
                detail.getFotoUrl(),
                detail.getTelefono(),
                detail.getDescripcion(),
                detail.getTipoPerfil(),
                valueOrZero(totalPublicaciones));
    }

    @Transactional
    public UsuarioResponseDTO crearUsuario(UsuarioRequestDTO request) {
        validarObligatorio(request.correo(), "El correo es obligatorio");
        validarObligatorio(request.password(), "La password es obligatoria");
        validarObligatorio(request.rol(), "El rol es obligatorio");
        validarObligatorio(request.tipoPerfil(), "El tipoPerfil es obligatorio");
        validarObligatorio(request.nombre(), "El nombre es obligatorio");

        if (usuarioRepository.existsCorreoIncludingDeleted(request.correo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo ya esta registrado");
        }

        Rol rol = rolRepository.findByNombreIgnoreCase(request.rol().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol invalido"));

        NombreTipoPerfil tipoPerfilEnum = parseTipoPerfil(request.tipoPerfil());
        TipoPerfil tipoPerfil = tipoPerfilRepository.findByNombre(tipoPerfilEnum)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "TipoPerfil invalido"));

        Usuario usuario = new Usuario();
        usuario.setCorreo(request.correo().trim());
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setEstadoConexion(false);
        usuario.setRol(rol);

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        Perfil perfil = new Perfil();
        perfil.setUsuario(usuarioGuardado);
        perfil.setNombre(request.nombre().trim());
        perfil.setApellido(normalizeNullable(request.apellido()));
        perfil.setTelefono(normalizeNullable(request.telefono()));
        perfil.setDescripcion(normalizeNullable(request.descripcion()));
        perfil.setFotoUrl(null);
        perfil.setTipoPerfil(tipoPerfil);
        perfil.setSaldoCreditos(1000); // Asegurar que inicie en 1000 créditos
        perfilRepository.save(perfil);

        // Guardar la transacción de regalo/bono de bienvenida
        TransaccionCredito transaccion = TransaccionCredito.builder()
                .usuario(usuarioGuardado)
                .cantidad(1000)
                .tipo(TipoTransaccion.REGISTRO_INICIAL)
                .descripcion("Bono de bienvenida de 1000 créditos")
                .build();
        transaccionCreditoRepository.save(transaccion);

        return obtenerDetalle(usuarioGuardado.getId());
    }

    @Transactional
    public UsuarioResponseDTO actualizarBasico(UUID id, UsuarioPatchRequestDTO request) {
        UsuarioRepository.UsuarioDetailProjection actual = usuarioRepository.findDetalleAdminById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (perfilRepository.findIdByUsuarioIdIncludingDeleted(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado");
        }

        String correoFinal = normalizeNullable(request.correo()) != null ? request.correo().trim() : actual.getCorreo();
        if (!actual.getCorreo().equalsIgnoreCase(correoFinal)
                && usuarioRepository.existsCorreoByOtroUsuarioIncludingDeleted(correoFinal, id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo ya esta registrado");
        }

        String nombreFinal = chooseUpdatedValue(request.nombre(), actual.getNombre());
        if (nombreFinal == null || nombreFinal.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre no puede quedar vacio");
        }

        String apellidoFinal = chooseUpdatedNullable(request.apellido(), actual.getApellido());
        String telefonoFinal = chooseUpdatedNullable(request.telefono(), actual.getTelefono());
        String descripcionFinal = chooseUpdatedNullable(request.descripcion(), actual.getDescripcion());

        NombreTipoPerfil tipoPerfilEnum = parseTipoPerfilOrDefault(request.tipoPerfil(), actual.getTipoPerfil());
        TipoPerfil tipoPerfil = tipoPerfilRepository.findByNombre(tipoPerfilEnum)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "TipoPerfil invalido"));

        usuarioRepository.actualizarCorreo(id, correoFinal);
        perfilRepository.actualizarPerfilBasico(id, nombreFinal.trim(), apellidoFinal, telefonoFinal, descripcionFinal,
                tipoPerfil.getId());

        return obtenerDetalle(id);
    }

    @Transactional
    public void desactivar(UUID id) {
        int updated = usuarioRepository.desactivarUsuario(id);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
    }

    @Transactional
    public UsuarioResponseDTO activar(UUID id) {
        int updated = usuarioRepository.activarUsuario(id);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        return obtenerDetalle(id);
    }

    private UsuarioStatsDTO construirStats() {
        return new UsuarioStatsDTO(
                valueOrZero(usuarioRepository.countTotalUsuariosIncludingDeleted()),
                valueOrZero(usuarioRepository.countUsuariosActivos()),
                valueOrZero(usuarioRepository.countUsuariosInactivos()),
                valueOrZero(publicacionRepository.countPublicacionesActivas()));
    }

    private NombreTipoPerfil parseTipoPerfil(String raw) {
        try {
            return NombreTipoPerfil.valueOf(raw.trim().toUpperCase());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TipoPerfil invalido");
        }
    }

    private NombreTipoPerfil parseTipoPerfilOrDefault(String incoming, String actual) {
        if (incoming == null || incoming.isBlank()) {
            return parseTipoPerfil(actual);
        }
        return parseTipoPerfil(incoming);
    }

    private void validarObligatorio(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String chooseUpdatedValue(String incoming, String current) {
        if (incoming == null) {
            return current;
        }
        return incoming.trim();
    }

    private String chooseUpdatedNullable(String incoming, String current) {
        if (incoming == null) {
            return current;
        }
        String trimmed = incoming.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
