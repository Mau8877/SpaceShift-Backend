package com.sw.api.modules.chat.service;

import com.sw.api.modules.chat.dto.ChatDTO;
import com.sw.api.modules.chat.dto.MensajeDTO;
import com.sw.api.modules.chat.model.Conversacion;
import com.sw.api.modules.chat.model.EstadoMensaje;
import com.sw.api.modules.chat.model.Mensaje;
import com.sw.api.modules.chat.model.ParticipanteConversacion;
import com.sw.api.modules.chat.model.ParticipanteConversacionId;
import com.sw.api.modules.chat.model.RolParticipante;
import com.sw.api.modules.chat.repository.ConversacionRepository;
import com.sw.api.modules.chat.repository.MensajeRepository;
import com.sw.api.modules.chat.repository.ParticipanteConversacionRepository;
import com.sw.api.modules.publicacion.model.Publicacion;
import com.sw.api.modules.publicacion.repository.PublicacionRepository;
import com.sw.api.modules.usuario.model.Perfil;
import com.sw.api.modules.usuario.model.Usuario;
import com.sw.api.modules.usuario.repository.PerfilRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ParticipanteConversacionRepository participanteConversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final PerfilRepository perfilRepository;
    private final ConversacionRepository conversacionRepository;
    private final PublicacionRepository publicacionRepository;

    public List<ChatDTO> obtenerChatsUsuario(Usuario usuarioAutenticado) {
        List<ParticipanteConversacion> participaciones = participanteConversacionRepository
                .findAllConversacionesByUsuarioIdOrderByActualizadoEnDesc(usuarioAutenticado.getId());

        return participaciones.stream().map(participacion -> {
            Conversacion conversacion = participacion.getConversacion();

            List<ParticipanteConversacion> otrosParticipantes = participanteConversacionRepository
                    .findOtrosParticipantes(conversacion.getId(), usuarioAutenticado.getId());

            UUID otroUsuarioId = null;
            String nombreOtroUsuario = "Desconocido";
            String fotoOtroUsuario = "";

            if (!otrosParticipantes.isEmpty()) {
                Usuario otroUsuario = otrosParticipantes.get(0).getUsuario();
                otroUsuarioId = otroUsuario.getId();

                Optional<Perfil> perfilOpt = perfilRepository.findByUsuario(otroUsuario);
                if (perfilOpt.isPresent()) {
                    Perfil perfil = perfilOpt.get();
                    nombreOtroUsuario = perfil.getNombre();
                    if (perfil.getApellido() != null && !perfil.getApellido().isEmpty()) {
                        nombreOtroUsuario += " " + perfil.getApellido();
                    }
                    fotoOtroUsuario = perfil.getFotoUrl() != null ? perfil.getFotoUrl() : "";
                } else {
                    nombreOtroUsuario = otroUsuario.getCorreo();
                }
            }

            String titulo = "Chat de Inmueble";
            if (conversacion.getPropiedad() != null && conversacion.getPropiedad().getTipoInmueble() != null) {
                titulo = conversacion.getPropiedad().getTipoInmueble();
            }

            int mensajesSinLeer = mensajeRepository.countByConversacionIdAndEstadoNotAndRemitenteIdNot(
                    conversacion.getId(), EstadoMensaje.LEIDO, usuarioAutenticado.getId());

            return new ChatDTO(
                    conversacion.getId(),
                    titulo,
                    otroUsuarioId,
                    nombreOtroUsuario,
                    fotoOtroUsuario,
                    conversacion.getActualizadoEn(),
                    mensajesSinLeer
            );
        }).collect(Collectors.toList());
    }

    public void marcarMensajesComoLeidos(UUID conversacionId, UUID usuarioId) {
        mensajeRepository.marcarComoLeidos(conversacionId, usuarioId);
    }

    public Page<MensajeDTO> obtenerHistorialPaginado(UUID conversacionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Mensaje> mensajes = mensajeRepository.findByConversacionIdOrderByCreadoEnDesc(conversacionId, pageable);

        return mensajes.map(m -> new MensajeDTO(
                m.getId(),
                m.getConversacion().getId(),
                m.getRemitente().getId(),
                m.getContenido(),
                m.getEstado(),
                m.getCreadoEn()
        ));
    }

    public ChatDTO crearChat(UUID publicacionId, Usuario cliente) {
        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> new RuntimeException("Publicacion no encontrada"));

        Usuario propietario = publicacion.getUsuario();
        
        // 1. Check if conversation already exists
        Optional<Conversacion> existingConversacion = conversacionRepository.findByInmuebleAndParticipantes(
            publicacion.getInmueble().getId(), 
            cliente.getId(), 
            propietario.getId()
        );

        Conversacion conversacion;
        
        if (existingConversacion.isPresent()) {
            conversacion = existingConversacion.get();
        } else {
            // Create a new conversation if it doesn't exist
            conversacion = new Conversacion();
            conversacion.setPropiedad(publicacion.getInmueble());
            conversacion = conversacionRepository.save(conversacion);

            ParticipanteConversacion partCliente = new ParticipanteConversacion();
            partCliente.setId(new ParticipanteConversacionId(conversacion.getId(), cliente.getId()));
            partCliente.setConversacion(conversacion);
            partCliente.setUsuario(cliente);
            partCliente.setRol(RolParticipante.CLIENTE);
            participanteConversacionRepository.save(partCliente);

            if (!propietario.getId().equals(cliente.getId())) {
                ParticipanteConversacion partPropietario = new ParticipanteConversacion();
                partPropietario.setId(new ParticipanteConversacionId(conversacion.getId(), propietario.getId()));
                partPropietario.setConversacion(conversacion);
                partPropietario.setUsuario(propietario);
                partPropietario.setRol(RolParticipante.PROPIETARIO);
                participanteConversacionRepository.save(partPropietario);
            }
        }

        String nombrePropietario = "Desconocido";
        String fotoPropietario = "";
        Optional<Perfil> perfilProp = perfilRepository.findByUsuario(propietario);
        if (perfilProp.isPresent()) {
            Perfil p = perfilProp.get();
            nombrePropietario = p.getNombre();
            if (p.getApellido() != null && !p.getApellido().isEmpty()) {
                nombrePropietario += " " + p.getApellido();
            }
            fotoPropietario = p.getFotoUrl() != null ? p.getFotoUrl() : "";
        }

        return new ChatDTO(
                conversacion.getId(),
                publicacion.getTitulo(),
                propietario.getId(),
                nombrePropietario,
                fotoPropietario,
                conversacion.getActualizadoEn(),
                0
        );
    }
}
