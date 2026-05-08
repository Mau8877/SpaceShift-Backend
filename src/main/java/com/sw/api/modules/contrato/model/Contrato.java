package com.sw.api.modules.contrato.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import com.sw.api.modules.inmueble.model.Inmueble;
import com.sw.api.modules.publicacion.model.Publicacion;
import com.sw.api.modules.usuario.model.Usuario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "contrato")
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "id_inmueble", nullable = false)
    private Inmueble inmueble;

    @ManyToOne
    @JoinColumn(name = "id_publicacion")
    private Publicacion publicacion;

    @ManyToOne
    @JoinColumn(name = "id_propietario", nullable = false)
    private Usuario propietario;

    @ManyToOne
    @JoinColumn(name = "id_cliente", nullable = false)
    private Usuario cliente;

    @Column(name = "tipo_contrato", nullable = false, length = 50)
    private String tipoContrato;

    @Column(name = "estado_contrato", nullable = false, length = 50)
    private String estadoContrato;

    @Column(name = "monto_acordado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoAcordado;

    @Column(nullable = false, length = 10)
    private String moneda;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "cantidad_huespedes")
    private Integer cantidadHuespedes;

    private Integer noches;

    @Column(name = "documento_url", length = 500)
    private String documentoUrl;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    public void prePersist() {
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
    }
}
