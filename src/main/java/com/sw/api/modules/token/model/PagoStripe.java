package com.sw.api.modules.token.model;

import com.sw.api.modules.usuario.model.Usuario;
import com.sw.api.shared.model.Auditable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "pago_stripe")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted = false")
public class PagoStripe extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "paquete_credito_id", nullable = false)
    private PaqueteCredito paqueteCredito;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaccion_credito_id")
    private TransaccionCredito transaccionCredito;

    @Column(name = "stripe_session_id", unique = true, nullable = false)
    private String stripeSessionId;

    @Column(name = "stripe_payment_intent_id", unique = true)
    private String stripePaymentIntentId;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "moneda", nullable = false, length = 10)
    private String moneda;

    @Column(name = "estado_pago", nullable = false, length = 50)
    private String estadoPago; // 'PENDIENTE', 'COMPLETADO', 'FALLIDO'
}
