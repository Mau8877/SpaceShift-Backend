package com.sw.api.modules.token.model;

import com.sw.api.shared.model.Auditable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "paquete_credito")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted = false")
public class PaqueteCredito extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nombre_paquete", nullable = false, length = 100)
    private String nombrePaquete;

    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "creditos_paquetes", nullable = false)
    private Integer creditosPaquetes;
}
