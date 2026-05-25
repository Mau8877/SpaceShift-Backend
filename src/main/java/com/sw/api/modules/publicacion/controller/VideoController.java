package com.sw.api.modules.publicacion.controller;

import com.sw.api.modules.publicacion.dto.VideoResponseDTO;
import com.sw.api.modules.publicacion.dto.VideoUploadRequestDTO;
import com.sw.api.modules.publicacion.model.VideoPublicacion;
import com.sw.api.modules.publicacion.service.VideoService;
import com.sw.api.modules.usuario.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @PostMapping("/publicaciones/{idPublicacion}")
    public ResponseEntity<?> registrarVideo(
            @PathVariable UUID idPublicacion,
            @RequestBody VideoUploadRequestDTO dto,
            Authentication authentication) {
        try {
            Usuario usuario = (Usuario) authentication.getPrincipal();
            VideoPublicacion video = videoService.registrarVideoParaProcesar(
                    idPublicacion,
                    dto.urlVideo(),
                    dto.nombreArchivo(),
                    dto.tamanoBytes(),
                    dto.duracionSegundos(),
                    usuario.getId()
            );
            return new ResponseEntity<>(mapToDTO(video), HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/publicaciones/{idPublicacion}")
    public ResponseEntity<List<VideoResponseDTO>> obtenerVideosDePublicacion(@PathVariable UUID idPublicacion) {
        List<VideoPublicacion> videos = videoService.listarVideosPorPublicacion(idPublicacion);
        List<VideoResponseDTO> dtos = videos.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{idVideo}/estado")
    public ResponseEntity<VideoResponseDTO> obtenerEstadoVideo(@PathVariable UUID idVideo) {
        VideoPublicacion video = videoService.obtenerPorId(idVideo);
        return ResponseEntity.ok(mapToDTO(video));
    }

    private VideoResponseDTO mapToDTO(VideoPublicacion video) {
        return new VideoResponseDTO(
                video.getId(),
                video.getPublicacion().getId(),
                video.getUrlVideo(),
                video.getUrlModelo3D(),
                video.getDuracionSegundos(),
                video.getCreditosConsumidos(),
                video.getEstadoProcesamiento(),
                video.getNombreArchivo(),
                video.getTamanoBytes(),
                video.getErrorMensaje(),
                video.getCreatedDate() // Heredado de Auditable
        );
    }
}
