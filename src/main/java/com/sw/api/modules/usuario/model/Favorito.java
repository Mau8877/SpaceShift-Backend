package com.sw.api.modules.usuario.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sw.api.modules.publicacion.model.Publicacion;
import com.sw.api.shared.model.Auditable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "favorito")
public class Favorito extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_publicacion", nullable = false)
    private Publicacion publicacion;

    @Column(name = "fecha_agregado", nullable = false)
    private LocalDateTime fechaAgregado;

    @PrePersist
    public void prePersistFavorito() {
        if (this.fechaAgregado == null) {
            this.fechaAgregado = LocalDateTime.now();
        }
    }
}
