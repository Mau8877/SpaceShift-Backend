package com.sw.api.modules.chat.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.sw.api.modules.usuario.model.Usuario;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "token_dispositivo")
public class TokenDispositivo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_fcm", nullable = false, unique = true)
    private String tokenFcm;

    @Enumerated(EnumType.STRING)
    @Column(name = "plataforma", nullable = false)
    private PlataformaDispositivo plataforma;

    @Column(name = "ultimo_uso", nullable = false)
    private LocalDateTime ultimoUso = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        ultimoUso = LocalDateTime.now();
    }
}
