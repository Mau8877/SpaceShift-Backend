package com.sw.api.modules.publicacion.service;

import com.sw.api.modules.publicacion.dto.CotizacionResponseDTO;
import com.sw.api.modules.publicacion.model.EstadoProcesamiento;
import com.sw.api.modules.publicacion.model.Publicacion;
import com.sw.api.modules.publicacion.model.VideoPublicacion;
import com.sw.api.modules.publicacion.repository.PublicacionRepository;
import com.sw.api.modules.publicacion.repository.VideoPublicacionRepository;
import com.sw.api.modules.usuario.model.Usuario;
import com.sw.api.modules.usuario.repository.UsuarioRepository;
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
    private final UsuarioRepository usuarioRepository;
    private final TokenService tokenService;
    private final VideoProcessor videoProcessor;
    private final RunpodService runpodService;

    private static final int CREDIT_FACTOR = 2; // 2 créditos por segundo de video

    /**
     * Calcula el costo de procesar un video de la duración indicada sin debitar créditos.
     * Permite al frontend mostrar el precio antes de que el usuario confirme.
     */
    @Transactional(readOnly = true)
    public CotizacionResponseDTO cotizar(Integer duracionSegundos, UUID usuarioId) {
        int costoCreditos = duracionSegundos * CREDIT_FACTOR;
        int saldoActual = tokenService.obtenerSaldo(usuarioId);
        return new CotizacionResponseDTO(
                duracionSegundos,
                CREDIT_FACTOR,
                costoCreditos,
                saldoActual,
                saldoActual >= costoCreditos
        );
    }

    @Transactional
    public VideoPublicacion registrarVideoParaProcesar(UUID publicacionId, String urlVideo, String nombreArchivo, Long tamanoBytes, Integer duracionSegundos, UUID usuarioId) {
        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> new RuntimeException("Publicación no encontrada."));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

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
                .usuario(usuario)
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
                String status = response.getStatus();

                if ("COMPLETED".equalsIgnoreCase(status)) {
                    video.setEstadoProcesamiento(EstadoProcesamiento.COMPLETADO);

                    if (response.getOutput() != null && response.getOutput().getAssets() != null) {
                        RunpodResponse.Assets assets = response.getOutput().getAssets();
                        asignarModelo(video, assets.getModel());
                        video.setUrlJsonModelo(assets.getMetadata());
                        video.setUrlPreviewWebp(assets.getPreview());
                    }

                    videoPublicacionRepository.save(video);

                } else if ("FAILED".equalsIgnoreCase(status)
                        || "CANCELLED".equalsIgnoreCase(status)
                        || "TIMED_OUT".equalsIgnoreCase(status)) {
                    video.setEstadoProcesamiento(EstadoProcesamiento.FALLIDO);
                    video.setErrorMensaje("La reconstrucción 3D en Runpod falló (estado: " + status + ").");
                    reembolsar(video, "Reembolso por fallo en Runpod: " + video.getNombreArchivo());
                    videoPublicacionRepository.save(video);
                }
            } catch (Exception e) {
                log.error("Error consultando estado de Runpod para el Job {}", video.getRunpodJobId(), e);
            }
        }

        return video;
    }

    /**
     * Mapea la URL del modelo según su extensión. RunPod devuelve el modelo en
     * {@code assets.model} tanto para .splat como para .sog, solo cambia la extensión.
     */
    private void asignarModelo(VideoPublicacion video, String modelUrl) {
        if (modelUrl == null) {
            return;
        }
        video.setUrlModelo3D(modelUrl); // Mantenemos por compatibilidad
        if (modelUrl.toLowerCase().endsWith(".sog")) {
            video.setUrlSog(modelUrl);
        } else if (modelUrl.toLowerCase().endsWith(".splat")) {
            video.setUrlSplat(modelUrl);
        }
    }

    /**
     * Reembolsa los créditos consumidos al usuario que pagó el procesamiento.
     */
    private void reembolsar(VideoPublicacion video, String descripcion) {
        if (video.getUsuario() == null) {
            log.warn("Video {} sin usuario asociado; no se puede reembolsar.", video.getId());
            return;
        }
        tokenService.acreditarCreditos(
                video.getUsuario().getId(),
                video.getCreditosConsumidos(),
                descripcion,
                TipoTransaccion.REEMBOLSO
        );
    }
}
