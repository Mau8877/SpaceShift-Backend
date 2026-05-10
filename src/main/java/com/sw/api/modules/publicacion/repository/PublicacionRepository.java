package com.sw.api.modules.publicacion.repository;

import com.sw.api.modules.publicacion.model.Publicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PublicacionRepository extends JpaRepository<Publicacion, UUID> {

    @Query(value = """
            SELECT id_usuario AS usuarioId,
                   COUNT(*) AS totalPublicaciones
            FROM publicacion
            WHERE deleted = FALSE
              AND id_usuario IN (:usuarioIds)
            GROUP BY id_usuario
            """, nativeQuery = true)
    List<PublicacionCountProjection> countPublicacionesPorUsuarios(@Param("usuarioIds") List<UUID> usuarioIds);

    @Query(value = "SELECT COUNT(*) FROM publicacion WHERE deleted = FALSE", nativeQuery = true)
    Long countPublicacionesActivas();

    @Query(value = "SELECT COUNT(*) FROM publicacion WHERE deleted = FALSE AND id_usuario = :usuarioId", nativeQuery = true)
    Long countPublicacionesActivasByUsuarioId(@Param("usuarioId") UUID usuarioId);

    List<Publicacion> findByUsuarioId(UUID usuarioId);

    interface PublicacionCountProjection {
        UUID getUsuarioId();

        Long getTotalPublicaciones();
    }
}
