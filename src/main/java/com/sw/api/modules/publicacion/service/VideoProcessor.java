package com.sw.api.modules.publicacion.service;

import com.sw.api.modules.publicacion.model.EstadoProcesamiento;
import com.sw.api.modules.publicacion.model.Formato3D;
import com.sw.api.modules.publicacion.model.VideoPublicacion;
import com.sw.api.modules.publicacion.repository.VideoPublicacionRepository;
import com.sw.api.modules.token.model.TipoTransaccion;
import com.sw.api.modules.token.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sw.api.modules.video_processing.service.RunpodService;
import com.sw.api.modules.video_processing.service.S3Service;
import com.sw.api.modules.video_processing.dto.RunpodResponse;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoProcessor {

    private final VideoPublicacionRepository videoPublicacionRepository;
    private final TokenService tokenService;
    private final RunpodService runpodService;
    private final S3Service s3Service;

    @Async
    @Transactional
    public void procesarVideo3DAsync(UUID videoId, UUID usuarioId) {
        log.info("Iniciando procesamiento de Runpod para el video: {}", videoId);
        try {
            VideoPublicacion video = videoPublicacionRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video no encontrado."));

            // 1. Generar Presigned GET URL
            String presignedUrl = s3Service.generatePresignedGetUrl(video.getUrlVideo());

            // 2. Enviar a Runpod (al endpoint correspondiente al formato elegido)
            boolean splat = video.getFormato() == Formato3D.SPLAT;
            RunpodResponse runpodResponse = runpodService.processVideo(presignedUrl, splat);

            // 3. Guardar el Job ID y dejar el estado en PROCESANDO
            video.setRunpodJobId(runpodResponse.getId());
            videoPublicacionRepository.save(video);
            
            log.info("Video {} enviado a Runpod. Job ID: {}", videoId, runpodResponse.getId());

        } catch (Exception e) {
            log.error("Error inesperado al enviar a Runpod el video {}. Reembolsando tokens.", videoId, e);
            
            // Si falla el envío a Runpod, reembolsamos
            VideoPublicacion video = videoPublicacionRepository.findById(videoId).orElse(null);
            if (video != null) {
                video.setEstadoProcesamiento(EstadoProcesamiento.FALLIDO);
                video.setErrorMensaje("No se pudo iniciar el trabajo en Runpod: " + e.getMessage());

                UUID idReembolso = video.getUsuario() != null ? video.getUsuario().getId() : usuarioId;
                tokenService.acreditarCreditos(
                        idReembolso,
                        video.getCreditosConsumidos(),
                        "Reembolso por fallo en inicio de procesamiento: " + video.getNombreArchivo(),
                        TipoTransaccion.REEMBOLSO
                );
                videoPublicacionRepository.save(video);
            }
        }
    }
}
