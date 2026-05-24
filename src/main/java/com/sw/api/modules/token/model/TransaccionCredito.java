package com.sw.api.modules.token.model;

import com.sw.api.modules.usuario.model.Usuario;
import com.sw.api.modules.publicacion.model.Publicacion;
import com.sw.api.shared.model.Auditable;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "transaccion_credito")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransaccionCredito extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private Integer cantidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoTransaccion tipo;

    private String descripcion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_publicacion")
    private Publicacion publicacion;
}
