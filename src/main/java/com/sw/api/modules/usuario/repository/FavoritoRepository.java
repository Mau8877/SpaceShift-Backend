package com.sw.api.modules.usuario.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sw.api.modules.usuario.model.Favorito;

@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, UUID> {
    
    List<Favorito> findByUsuarioIdOrderByFechaAgregadoDesc(UUID usuarioId);
    
    Optional<Favorito> findByUsuarioIdAndPublicacionId(UUID usuarioId, UUID publicacionId);
    
    boolean existsByUsuarioIdAndPublicacionId(UUID usuarioId, UUID publicacionId);
    
    void deleteByUsuarioIdAndPublicacionId(UUID usuarioId, UUID publicacionId);
}
