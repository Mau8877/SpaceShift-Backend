package com.sw.api.modules.usuario.repository;

import com.sw.api.modules.usuario.model.Perfil;
import com.sw.api.modules.usuario.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PerfilRepository extends JpaRepository<Perfil, UUID> {
    Optional<Perfil> findByUsuario(Usuario usuario);
}
