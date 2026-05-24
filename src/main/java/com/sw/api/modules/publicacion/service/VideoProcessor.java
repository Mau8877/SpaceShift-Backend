package com.sw.api.modules.publicacion.service;

import com.sw.api.modules.publicacion.model.EstadoProcesamiento;
import com.sw.api.modules.publicacion.model.VideoPublicacion;
import com.sw.api.modules.publicacion.repository.VideoPublicacionRepository;
import com.sw.api.modules.token.model.TipoTransaccion;
import com.sw.api.modules.token.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoProcessor {

    private final VideoPublicacionRepository videoPublicacionRepository;
    private final TokenService tokenService;

    @Async
    @Transactional
    public void procesarVideo3DAsync(UUID videoId, UUID usuarioId) {
        log.info("Iniciando procesamiento asíncrono del video: {}", videoId);
        try {
            // Simular el procesamiento pesado de reconstrucción 3D (15 segundos)
            Thread.sleep(15000);

            VideoPublicacion video = videoPublicacionRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video no encontrado."));

            // Simular un 90% de probabilidad de éxito
            boolean exito = Math.random() < 0.9;

            if (exito) {
                video.setEstadoProcesamiento(EstadoProcesamiento.COMPLETADO);
                video.setUrlModelo3D("https://res.cloudinary.com/demo/image/upload/spaceshift_gltf_sample.glb");
                log.info("Procesamiento de video {} completado con éxito.", videoId);
            } else {
                video.setEstadoProcesamiento(EstadoProcesamiento.FALLIDO);
                video.setErrorMensaje("La reconstrucción 3D falló. Posible causa: Movimiento de cámara demasiado rápido o falta de iluminación en la escena.");
                log.error("Procesamiento de video {} falló. Iniciando reembolso.", videoId);

                // Reembolsar créditos consumidos
                tokenService.acreditarCreditos(
                        usuarioId,
                        video.getCreditosConsumidos(),
                        "Reembolso por fallo en renderizado de video: " + video.getNombreArchivo(),
                        TipoTransaccion.REEMBOLSO
                );
            }

            videoPublicacionRepository.save(video);

        } catch (InterruptedException e) {
            log.error("El procesamiento de video fue interrumpido.", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error inesperado en el procesamiento de video.", e);
        }
    }
}
