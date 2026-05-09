package com.sw.api.modules.usuario.repository;

import com.sw.api.modules.usuario.enums.NombreTipoPerfil;
import com.sw.api.modules.usuario.model.TipoPerfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TipoPerfilRepository extends JpaRepository<TipoPerfil, UUID> {
    Optional<TipoPerfil> findByNombre(NombreTipoPerfil nombre);
}
