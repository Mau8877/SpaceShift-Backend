package com.sw.api.modules.usuario.model;

import com.sw.api.modules.usuario.enums.NombreTipoPerfil;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "tipos_perfil")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoPerfil {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private NombreTipoPerfil nombre;
}
