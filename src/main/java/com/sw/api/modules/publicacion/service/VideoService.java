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

    @Transactional(readOnly = true)
    public VideoPublicacion obtenerPorId(UUID videoId) {
        return videoPublicacionRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video no encontrado con ID: " + videoId));
    }
}
