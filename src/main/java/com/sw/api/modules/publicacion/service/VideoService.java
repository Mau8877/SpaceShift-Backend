package com.sw.api.modules.publicacion.service;

import com.sw.api.modules.publicacion.dto.CotizacionResponseDTO;
import com.sw.api.modules.publicacion.model.EstadoProcesamiento;
import com.sw.api.modules.publicacion.model.Formato3D;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

    private static final int CREDIT_FACTOR = 2; // 2 créditos por segundo de video (base SOG)
    private static final double SPLAT_MULTIPLIER = 1.5; // SPLAT cuesta 1.5x respecto a SOG

    /**
     * Calcula el costo en créditos de un video según su duración y formato.
     * SOG es el costo base; SPLAT aplica un recargo del 50% (1.5x).
     */
    private int calcularCosto(Integer duracionSegundos, Formato3D formato) {
        int base = duracionSegundos * CREDIT_FACTOR;
        if (formato == Formato3D.SPLAT) {
            return (int) Math.round(base * SPLAT_MULTIPLIER);
        }
        return base;
    }

    /**
     * Calcula el costo de procesar un video de la duración y formato indicados sin
     * debitar créditos. Permite al frontend mostrar el precio antes de confirmar.
     */
    @Transactional(readOnly = true)
    public CotizacionResponseDTO cotizar(Integer duracionSegundos, Formato3D formato, UUID usuarioId) {
        Formato3D fmt = formato != null ? formato : Formato3D.SOG;
        int costoCreditos = calcularCosto(duracionSegundos, fmt);
        int saldoActual = tokenService.obtenerSaldo(usuarioId);
        return new CotizacionResponseDTO(
                duracionSegundos,
                CREDIT_FACTOR,
                costoCreditos,
                saldoActual,
                saldoActual >= costoCreditos,
                fmt
        );
    }

    @Transactional
    public VideoPublicacion registrarVideoParaProcesar(UUID publicacionId, String urlVideo, String nombreArchivo, Long tamanoBytes, Integer duracionSegundos, Formato3D formato, UUID usuarioId) {
        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> new RuntimeException("Publicación no encontrada."));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        Formato3D fmt = formato != null ? formato : Formato3D.SOG;
        int costoCreditos = calcularCosto(duracionSegundos, fmt);

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
                .formato(fmt)
                .build();

        VideoPublicacion videoGuardado = videoPublicacionRepository.save(video);

        // Disparar el procesamiento asíncrono SOLO después de que la transacción haga commit.
        // Si lo lanzáramos aquí directamente, el hilo @Async podría ejecutarse antes del commit
        // y no encontrar la fila recién insertada (race condition).
        final UUID videoId = videoGuardado.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    videoProcessor.procesarVideo3DAsync(videoId, usuarioId);
                }
            });
        } else {
            videoProcessor.procesarVideo3DAsync(videoId, usuarioId);
        }

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
                boolean splat = video.getFormato() == Formato3D.SPLAT;
                RunpodResponse response = runpodService.checkStatus(video.getRunpodJobId(), splat);
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
