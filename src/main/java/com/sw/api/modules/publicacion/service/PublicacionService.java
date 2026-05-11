package com.sw.api.modules.publicacion.service;

import com.sw.api.modules.publicacion.dto.ImagenPublicacionDTO;
import com.sw.api.modules.publicacion.dto.PublicacionRequestDTO;
import com.sw.api.modules.publicacion.dto.PublicacionResponseDTO;
import com.sw.api.modules.publicacion.model.ImagenPublicacion;
import com.sw.api.modules.publicacion.model.Publicacion;
import com.sw.api.modules.publicacion.repository.PublicacionRepository;
import com.sw.api.modules.inmueble.model.Inmueble;
import com.sw.api.modules.inmueble.service.InmuebleService;
import com.sw.api.modules.usuario.model.Usuario;
import com.sw.api.modules.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PublicacionService {

    private final PublicacionRepository publicacionRepository;
    private final InmuebleService inmuebleService;
    private final UsuarioRepository usuarioRepository;

    public PublicacionService(PublicacionRepository publicacionRepository,
                              InmuebleService inmuebleService,
                              UsuarioRepository usuarioRepository) {
        this.publicacionRepository = publicacionRepository;
        this.inmuebleService = inmuebleService;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<String> obtenerTiposTransaccionUnicos() {
        return publicacionRepository.findDistinctTipoTransaccion();
    }

    @Transactional
    public PublicacionResponseDTO crear(PublicacionRequestDTO dto) {
        Usuario usuario = usuarioRepository.findById(dto.idUsuario())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + dto.idUsuario()));

        Inmueble inmueble = inmuebleService.obtenerEntidadPorId(dto.idInmueble());

        Publicacion publicacion = new Publicacion();
        publicacion.setUsuario(usuario);
        publicacion.setInmueble(inmueble);
        publicacion.setTitulo(dto.titulo());
        publicacion.setDescripcionGeneral(dto.descripcionGeneral());
        publicacion.setTipoTransaccion(dto.tipoTransaccion());
        publicacion.setPrecio(dto.precio());

        if (dto.moneda() != null && !dto.moneda().isBlank()) {
            publicacion.setMoneda(dto.moneda());
        }

        if (dto.estadoPublicacion() != null && !dto.estadoPublicacion().isBlank()) {
            publicacion.setEstadoPublicacion(dto.estadoPublicacion());
        }

        if (dto.imagenesUrls() != null && !dto.imagenesUrls().isEmpty()) {
            boolean isFirst = true;
            for (String url : dto.imagenesUrls()) {
                ImagenPublicacion imagen = new ImagenPublicacion();
                imagen.setUrlImage(url);
                imagen.setEsPortada(isFirst);
                publicacion.addImagen(imagen);
                isFirst = false;
            }
        }

        Publicacion guardado = publicacionRepository.save(publicacion);
        return mapToResponseDTO(guardado);
    }

    @Transactional(readOnly = true)
    public List<PublicacionResponseDTO> obtenerTodas() {
        return publicacionRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PublicacionResponseDTO> obtenerPorUsuarioId(UUID usuarioId) {
        return publicacionRepository.findByUsuarioId(usuarioId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PublicacionResponseDTO obtenerPorId(UUID id) {
        Publicacion publicacion = publicacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Publicación no encontrada"));
        return mapToResponseDTO(publicacion);
    }

    @Transactional
    public PublicacionResponseDTO actualizar(UUID id, PublicacionRequestDTO dto) {
        Publicacion publicacion = publicacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Publicación no encontrada"));

        if (dto.titulo() != null) publicacion.setTitulo(dto.titulo());
        if (dto.descripcionGeneral() != null) publicacion.setDescripcionGeneral(dto.descripcionGeneral());
        if (dto.tipoTransaccion() != null) publicacion.setTipoTransaccion(dto.tipoTransaccion());
        if (dto.precio() != null) publicacion.setPrecio(dto.precio());
        if (dto.moneda() != null) publicacion.setMoneda(dto.moneda());
        if (dto.estadoPublicacion() != null) publicacion.setEstadoPublicacion(dto.estadoPublicacion());

        if (dto.idInmueble() != null && !dto.idInmueble().equals(publicacion.getInmueble().getId())) {
            Inmueble nuevoInmueble = inmuebleService.obtenerEntidadPorId(dto.idInmueble());
            publicacion.setInmueble(nuevoInmueble);
        } else if (dto.inmueble() != null) {
            // Si el ID es el mismo, pero vienen detalles, los actualizamos
            inmuebleService.actualizar(publicacion.getInmueble().getId(), dto.inmueble());
        }

        if (dto.imagenesUrls() != null) {
            publicacion.getImagenes().clear();
            boolean isFirst = true;
            for (String url : dto.imagenesUrls()) {
                ImagenPublicacion imagen = new ImagenPublicacion();
                imagen.setUrlImage(url);
                imagen.setEsPortada(isFirst);
                publicacion.addImagen(imagen);
                isFirst = false;
            }
        }

        Publicacion modificado = publicacionRepository.save(publicacion);
        return mapToResponseDTO(modificado);
    }

    @Transactional
    public void eliminar(UUID id) {
        Publicacion publicacion = publicacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Publicación no encontrada"));
        
        // Gracias a CascadeType.ALL en las relaciones, esto borrará lógicamente:
        // 1. La Publicación
        // 2. Sus Imágenes asociadas
        // 3. El Inmueble asociado
        // 4. La Ubicación asociada al inmueble
        publicacionRepository.delete(publicacion);
    }

    private PublicacionResponseDTO mapToResponseDTO(Publicacion publicacion) {
        List<ImagenPublicacionDTO> imgs = publicacion.getImagenes().stream()
                .map(img -> new ImagenPublicacionDTO(img.getId(), img.getUrlImage(), img.getEsPortada()))
                .collect(Collectors.toList());

        return new PublicacionResponseDTO(
                publicacion.getId(),
                publicacion.getUsuario().getId(),
                publicacion.getUsuario().getCorreo(),
                inmuebleService.mapToDTO(publicacion.getInmueble()),
                publicacion.getTitulo(),
                publicacion.getDescripcionGeneral(),
                publicacion.getTipoTransaccion(),
                publicacion.getPrecio(),
                publicacion.getMoneda(),
                publicacion.getEstadoPublicacion(),
                publicacion.getFechaPublicacion(),
                imgs
        );
    }
}
