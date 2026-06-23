package com.sw.api.modules.contrato.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.sw.api.shared.model.Auditable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "pago_contrato")
public class PagoContrato extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_contrato", nullable = false)
    private Contrato contrato;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 10)
    private String moneda;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pago", nullable = false, length = 50)
    private TipoPago tipoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false, length = 50)
    private EstadoPago estadoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 50)
    private MetodoPago metodoPago;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "documento_comprobante_url", length = 500)
    private String documentoComprobanteUrl;

    @Column(name = "stripe_pago_id", length = 255)
    private String stripePagoId;
}
