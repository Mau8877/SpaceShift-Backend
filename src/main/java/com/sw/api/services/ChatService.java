package com.sw.api.services;

import com.sw.api.dtos.ChatDTO;
import com.sw.api.dtos.MensajeDTO;
import com.sw.api.models.Chat.Conversacion;
import com.sw.api.models.Chat.Mensaje;
import com.sw.api.models.Chat.ParticipanteConversacionId;
import com.sw.api.models.Chat.ParticipanteConversacion;
import com.sw.api.models.Publicacion;
import com.sw.api.models.Chat.RolParticipante;
import com.sw.api.models.Usuario;
import com.sw.api.repositories.ConversacionRepository;
import com.sw.api.repositories.MensajeRepository;
import com.sw.api.repositories.ParticipanteConversacionRepository;
import com.sw.api.repositories.PerfilRepository;
import com.sw.api.repositories.PublicacionRepository;
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
            
            // Buscar al oro usuario de la conversación para completar los datos
            List<ParticipanteConversacion> otrosParticipantes = participanteConversacionRepository
                    .findOtrosParticipantes(conversacion.getId(), usuarioAutenticado.getId());

            UUID otroUsuarioId = null;
            String nombreOtroUsuario = "Desconocido";
            String fotoOtroUsuario = "";

            if (!otrosParticipantes.isEmpty()) {
                Usuario otroUsuario = otrosParticipantes.get(0).getUsuario();
                otroUsuarioId = otroUsuario.getId();
                
                Optional<com.sw.api.models.Perfil> perfilOpt = perfilRepository.findByUsuario(otroUsuario);
                if (perfilOpt.isPresent()) {
                    com.sw.api.models.Perfil perfil = perfilOpt.get();
                    nombreOtroUsuario = perfil.getNombre();
                    if (perfil.getApellido() != null && !perfil.getApellido().isEmpty()) {
                        nombreOtroUsuario += " " + perfil.getApellido();
                    }
                    fotoOtroUsuario = perfil.getFotoUrl() != null ? perfil.getFotoUrl() : "";
                } else {
                    nombreOtroUsuario = otroUsuario.getCorreo(); // fallback
                }
            }
            
            String titulo = "Chat de Inmueble";
            if (conversacion.getPropiedad() != null && conversacion.getPropiedad().getTipoInmueble() != null) {
                 titulo = conversacion.getPropiedad().getTipoInmueble();
            }

            return new ChatDTO(
                    conversacion.getId(),
                    titulo,
                    otroUsuarioId, 
                    nombreOtroUsuario, 
                    fotoOtroUsuario,
                    conversacion.getActualizadoEn()
            );
        }).collect(Collectors.toList());
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

        // Validar si ya existe un chat (opcionalmente) o crear uno nuevo
        Conversacion conversacion = new Conversacion();
        conversacion.setPropiedad(publicacion.getInmueble());
        conversacion = conversacionRepository.save(conversacion);

        // Agregamos al cliente
        ParticipanteConversacion partCliente = new ParticipanteConversacion();
        partCliente.setId(new ParticipanteConversacionId(conversacion.getId(), cliente.getId()));
        partCliente.setConversacion(conversacion);
        partCliente.setUsuario(cliente);
        partCliente.setRol(RolParticipante.CLIENTE);
        participanteConversacionRepository.save(partCliente);

        // Agregamos al propietario de la publicacion
        Usuario propietario = publicacion.getUsuario();
        if (!propietario.getId().equals(cliente.getId())) {
            ParticipanteConversacion partPropietario = new ParticipanteConversacion();
            partPropietario.setId(new ParticipanteConversacionId(conversacion.getId(), propietario.getId()));
            partPropietario.setConversacion(conversacion);
            partPropietario.setUsuario(propietario);
            partPropietario.setRol(RolParticipante.PROPIETARIO);
            participanteConversacionRepository.save(partPropietario);
        }

        String nombrePropietario = "Desconocido";
        String fotoPropietario = "";
        Optional<com.sw.api.models.Perfil> perfilProp = perfilRepository.findByUsuario(propietario);
        if (perfilProp.isPresent()) {
            com.sw.api.models.Perfil p = perfilProp.get();
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
                conversacion.getActualizadoEn()
        );
    }
}
