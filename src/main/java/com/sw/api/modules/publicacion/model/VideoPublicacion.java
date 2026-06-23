package com.sw.api.modules.publicacion.model;

import com.sw.api.modules.usuario.model.Usuario;
import com.sw.api.shared.model.Auditable;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "video_publicacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoPublicacion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_publicacion", nullable = false)
    private Publicacion publicacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario; // Usuario que pagó/procesó el video (para reembolsos)

    @Column(name = "url_video", nullable = false, length = 500)
    private String urlVideo;

    @Column(name = "url_modelo_3d", length = 500)
    private String urlModelo3D; // Mantenemos por compatibilidad

    @Column(name = "url_splat", length = 500)
    private String urlSplat;

    @Column(name = "url_sog", length = 500)
    private String urlSog;

    @Column(name = "url_json_modelo", length = 500)
    private String urlJsonModelo;

    @Column(name = "url_preview_webp", length = 500)
    private String urlPreviewWebp;

    @Column(name = "duracion_segundos", nullable = false)
    private Integer duracionSegundos;

    @Column(name = "creditos_consumidos", nullable = false)
    private Integer creditosConsumidos;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_procesamiento", nullable = false, length = 50)
    private EstadoProcesamiento estadoProcesamiento = EstadoProcesamiento.PENDIENTE;

    @Column(name = "nombre_archivo", nullable = false)
    private String nombreArchivo;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    @Column(name = "error_mensaje", columnDefinition = "TEXT")
    private String errorMensaje;

    @Column(name = "runpod_job_id", length = 100)
    private String runpodJobId;

    // Formato de salida elegido al generar el recorrido. Determina el costo y el
    // endpoint de Runpod usado. Nullable: los videos previos se tratan como SOG.
    @Enumerated(EnumType.STRING)
    @Column(name = "formato", length = 20)
    private Formato3D formato;
}
