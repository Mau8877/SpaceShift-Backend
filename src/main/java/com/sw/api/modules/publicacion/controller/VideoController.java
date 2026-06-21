package com.sw.api.modules.publicacion.controller;

import com.sw.api.modules.publicacion.dto.CotizacionResponseDTO;
import com.sw.api.modules.publicacion.dto.VideoResponseDTO;
import com.sw.api.modules.publicacion.dto.VideoUploadRequestDTO;
import com.sw.api.modules.publicacion.model.VideoPublicacion;
import com.sw.api.modules.publicacion.service.VideoService;
import com.sw.api.modules.video_processing.service.S3Service;
import com.sw.api.modules.video_processing.dto.UploadUrlResponse;
import com.sw.api.modules.usuario.model.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Videos", description = "Operaciones de procesamiento de videos 3D (Subida, Tokens, Runpod)")
public class VideoController {

    private final VideoService videoService;
    private final S3Service s3Service;

    @Operation(summary = "Paso 1: Obtener URL pre-firmada", description = "Obtiene una URL de S3 temporal para subir el video directamente desde el cliente.")
    @GetMapping("/upload-url")
    public ResponseEntity<UploadUrlResponse> getUploadUrl(@RequestParam(defaultValue = ".mp4") String extension) {
        UploadUrlResponse response = s3Service.generatePresignedPutUrl(extension);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cotizar procesamiento", description = "Devuelve el costo en créditos de procesar un video de la duración indicada y si el usuario tiene saldo suficiente. No debita créditos.")
    @GetMapping("/cotizar")
    public ResponseEntity<CotizacionResponseDTO> cotizar(
            @RequestParam Integer duracionSegundos,
            Authentication authentication) {
        Usuario usuario = (Usuario) authentication.getPrincipal();
        return ResponseEntity.ok(videoService.cotizar(duracionSegundos, usuario.getId()));
    }

    @Operation(summary = "Paso 2: Registrar video y procesar", description = "Verifica saldo, cobra tokens e inicia la reconstrucción 3D en Runpod.")
    @PostMapping("/publicaciones/{idPublicacion}")
    public ResponseEntity<?> registrarVideo(
            @PathVariable UUID idPublicacion,
            @RequestBody VideoUploadRequestDTO dto,
            Authentication authentication) {
        try {
            Usuario usuario = (Usuario) authentication.getPrincipal();
            VideoPublicacion video = videoService.registrarVideoParaProcesar(
                    idPublicacion,
                    dto.keyS3(), // La propiedad fue renombrada a keyS3
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

    @Operation(summary = "Obtener videos de una publicación", description = "Lista todos los videos y modelos 3D asociados a una publicación.")
    @GetMapping("/publicaciones/{idPublicacion}")
    public ResponseEntity<List<VideoResponseDTO>> obtenerVideosDePublicacion(@PathVariable UUID idPublicacion) {
        List<VideoPublicacion> videos = videoService.listarVideosPorPublicacion(idPublicacion);
        List<VideoResponseDTO> dtos = videos.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Paso 3: Consultar estado (Polling)", description = "Revisa si Runpod ya finalizó el modelo 3D. Devuelve COMPLETADO o FALLIDO y maneja reembolsos.")
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
                video.getUrlSplat(),
                video.getUrlSog(),
                video.getUrlJsonModelo(),
                video.getUrlPreviewWebp(),
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
