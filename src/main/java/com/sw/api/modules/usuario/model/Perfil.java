package com.sw.api.modules.usuario.model;

import com.sw.api.shared.model.Auditable;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "perfil")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Perfil extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = true)
    private String apellido;

    private String fotoUrl;

    @Column(length = 30)
    private String telefono;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @OneToOne
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "id_tipo_perfil", nullable = false)
    private TipoPerfil tipoPerfil;

    @Column(name = "saldo_creditos")
    private Integer saldoCreditos = 1000;

    @Column(name = "wallet_address", length = 42)
    private String walletAddress;
}
