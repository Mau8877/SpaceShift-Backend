package com.sw.api.modules.publicacion.service;

import com.sw.api.modules.publicacion.model.EstadoProcesamiento;
import com.sw.api.modules.publicacion.model.Publicacion;
import com.sw.api.modules.publicacion.model.VideoPublicacion;
import com.sw.api.modules.publicacion.repository.PublicacionRepository;
import com.sw.api.modules.publicacion.repository.VideoPublicacionRepository;
import com.sw.api.modules.token.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sw.api.modules.video_processing.service.RunpodService;
import com.sw.api.modules.video_processing.dto.RunpodResponse;
import com.sw.api.modules.token.model.TipoTransaccion;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final VideoPublicacionRepository videoPublicacionRepository;
    private final PublicacionRepository publicacionRepository;
    private final TokenService tokenService;
    private final VideoProcessor videoProcessor;
    private final RunpodService runpodService;

    private static final int CREDIT_FACTOR = 2; // 2 créditos por segundo de video

    @Transactional
    public VideoPublicacion registrarVideoParaProcesar(UUID publicacionId, String urlVideo, String nombreArchivo, Long tamanoBytes, Integer duracionSegundos, UUID usuarioId) {
        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> new RuntimeException("Publicación no encontrada."));

        int costoCreditos = duracionSegundos * CREDIT_FACTOR;

        if (!tokenService.verificarSaldo(usuarioId, costoCreditos)) {
            throw new RuntimeException("Saldo de créditos insuficiente. Costo requerido: " + costoCreditos + " créditos.");
        }

        // Debitar los créditos preventivamente
        tokenService.debitarCreditos(
                usuarioId,
                costoCreditos,
                publicacionId,
                "Procesamiento de video: " + nombreArchivo + " (" + duracionSegundos + " segundos)"
        );

        // Guardar registro de video en estado PROCESANDO
        VideoPublicacion video = VideoPublicacion.builder()
                .publicacion(publicacion)
                .urlVideo(urlVideo)
                .duracionSegundos(duracionSegundos)
                .creditosConsumidos(costoCreditos)
                .estadoProcesamiento(EstadoProcesamiento.PROCESANDO)
                .nombreArchivo(nombreArchivo)
                .tamanoBytes(tamanoBytes)
                .build();

        VideoPublicacion videoGuardado = videoPublicacionRepository.save(video);

        // Disparar procesamiento asíncrono
        videoProcessor.procesarVideo3DAsync(videoGuardado.getId(), usuarioId);

        return videoGuardado;
    }

    @Transactional(readOnly = true)
    public List<VideoPublicacion> listarVideosPorPublicacion(UUID publicacionId) {
        return videoPublicacionRepository.findByPublicacionId(publicacionId);
    }

    @Transactional
    public VideoPublicacion obtenerPorId(UUID videoId) {
        VideoPublicacion video = videoPublicacionRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video no encontrado con ID: " + videoId));

        // Polling: Si el video esta PROCESANDO y tiene un Job de Runpod, consultamos su estado real
        if (video.getEstadoProcesamiento() == EstadoProcesamiento.PROCESANDO && video.getRunpodJobId() != null) {
            try {
                RunpodResponse response = runpodService.checkStatus(video.getRunpodJobId());
                
                if ("COMPLETED".equalsIgnoreCase(response.getStatus())) {
                    video.setEstadoProcesamiento(EstadoProcesamiento.COMPLETADO);
                    
                    if (response.getOutput() != null && response.getOutput().getAssets() != null) {
                        video.setUrlSplat(response.getOutput().getAssets().getModel());
                        video.setUrlJsonModelo(response.getOutput().getAssets().getMetadata());
                        video.setUrlPreviewWebp(response.getOutput().getAssets().getPreview());
                        // Mantenemos esto por compatibilidad
                        video.setUrlModelo3D(response.getOutput().getAssets().getModel());
                    }
                    
                    videoPublicacionRepository.save(video);
                    
                } else if ("FAILED".equalsIgnoreCase(response.getStatus())) {
                    video.setEstadoProcesamiento(EstadoProcesamiento.FALLIDO);
                    video.setErrorMensaje("La reconstrucción 3D en Runpod falló.");
                    
                    // Reembolso automático
                    tokenService.acreditarCreditos(
                            video.getPublicacion().getUsuario().getId(),
                            video.getCreditosConsumidos(),
                            "Reembolso por fallo en Runpod: " + video.getNombreArchivo(),
                            TipoTransaccion.REEMBOLSO
                    );
                    videoPublicacionRepository.save(video);
                }
            } catch (Exception e) {
                log.error("Error consultando estado de Runpod para el Job {}", video.getRunpodJobId(), e);
            }
        }

        return video;
    }
}
