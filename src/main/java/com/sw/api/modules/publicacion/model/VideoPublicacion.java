package com.sw.api.modules.publicacion.model;

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

    @Column(name = "url_video", nullable = false, length = 500)
    private String urlVideo;

    @Column(name = "url_modelo_3d", length = 500)
    private String urlModelo3D;

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
}
