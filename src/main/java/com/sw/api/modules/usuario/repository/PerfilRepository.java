package com.sw.api.modules.usuario.repository;

import com.sw.api.modules.usuario.model.Perfil;
import com.sw.api.modules.usuario.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PerfilRepository extends JpaRepository<Perfil, UUID> {
    Optional<Perfil> findByUsuario(Usuario usuario);

    Optional<Perfil> findByUsuarioId(UUID idUsuario);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Perfil p WHERE p.usuario.id = :idUsuario")
    Optional<Perfil> findByUsuarioIdForUpdate(@Param("idUsuario") UUID idUsuario);

    @Query(value = "SELECT id FROM perfil WHERE id_usuario = :idUsuario", nativeQuery = true)
    Optional<UUID> findIdByUsuarioIdIncludingDeleted(@Param("idUsuario") UUID idUsuario);

    @Modifying
    @Query(value = """
            UPDATE perfil
            SET nombre = :nombre,
                apellido = :apellido,
                telefono = :telefono,
                descripcion = :descripcion,
                id_tipo_perfil = :tipoPerfilId,
                last_modified_date = NOW()
            WHERE id_usuario = :idUsuario
            """, nativeQuery = true)
    int actualizarPerfilBasico(
            @Param("idUsuario") UUID idUsuario,
            @Param("nombre") String nombre,
            @Param("apellido") String apellido,
            @Param("telefono") String telefono,
            @Param("descripcion") String descripcion,
            @Param("tipoPerfilId") UUID tipoPerfilId);
}
