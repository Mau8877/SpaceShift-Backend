package com.sw.api.modules.usuario.repository;

import com.sw.api.modules.usuario.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByCorreo(String correo);

    @Query(value = """
            SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END
            FROM usuario
            WHERE LOWER(correo) = LOWER(:correo)
            """, nativeQuery = true)
    boolean existsCorreoIncludingDeleted(@Param("correo") String correo);

    @Query(value = """
            SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END
            FROM usuario
            WHERE LOWER(correo) = LOWER(:correo)
              AND id <> :id
            """, nativeQuery = true)
    boolean existsCorreoByOtroUsuarioIncludingDeleted(@Param("correo") String correo, @Param("id") UUID id);

    @Query(value = """
            SELECT u.id AS id,
                   u.correo AS correo,
                   p.nombre AS nombre,
                   p.apellido AS apellido,
                   p.telefono AS telefono,
                   (NOT u.deleted) AS estado,
                   u.estado_conexion AS estadoConexion,
                   r.nombre AS rol,
                   tp.nombre AS tipoPerfil,
                   COALESCE(pub.total_publicaciones, 0) AS totalPublicaciones
            FROM usuario u
            LEFT JOIN perfil p ON p.id_usuario = u.id
            LEFT JOIN tipos_perfil tp ON tp.id = p.id_tipo_perfil
            LEFT JOIN rol r ON r.id = u.id_rol
            LEFT JOIN (
                SELECT id_usuario, COUNT(*) AS total_publicaciones
                FROM publicacion
                WHERE deleted = FALSE
                GROUP BY id_usuario
            ) pub ON pub.id_usuario = u.id
            WHERE (
                :search IS NULL OR :search = ''
                OR CAST(u.id AS TEXT) ILIKE CONCAT('%', :search, '%')
                OR u.correo ILIKE CONCAT('%', :search, '%')
                OR COALESCE(p.nombre, '') ILIKE CONCAT('%', :search, '%')
                OR COALESCE(p.apellido, '') ILIKE CONCAT('%', :search, '%')
                OR COALESCE(p.telefono, '') ILIKE CONCAT('%', :search, '%')
                OR COALESCE(r.nombre, '') ILIKE CONCAT('%', :search, '%')
                OR COALESCE(CAST(tp.nombre AS TEXT), '') ILIKE CONCAT('%', :search, '%')
            )
              AND (
                :estado IS NULL
                OR (:estado = TRUE AND u.deleted = FALSE)
                OR (:estado = FALSE AND u.deleted = TRUE)
              )
              AND (
                :estadoConexion IS NULL
                OR u.estado_conexion = :estadoConexion
              )
            ORDER BY u.created_date DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM usuario u
            LEFT JOIN perfil p ON p.id_usuario = u.id
            LEFT JOIN tipos_perfil tp ON tp.id = p.id_tipo_perfil
            LEFT JOIN rol r ON r.id = u.id_rol
            WHERE (
                :search IS NULL OR :search = ''
                OR CAST(u.id AS TEXT) ILIKE CONCAT('%', :search, '%')
                OR u.correo ILIKE CONCAT('%', :search, '%')
                OR COALESCE(p.nombre, '') ILIKE CONCAT('%', :search, '%')
                OR COALESCE(p.apellido, '') ILIKE CONCAT('%', :search, '%')
                OR COALESCE(p.telefono, '') ILIKE CONCAT('%', :search, '%')
                OR COALESCE(r.nombre, '') ILIKE CONCAT('%', :search, '%')
                OR COALESCE(CAST(tp.nombre AS TEXT), '') ILIKE CONCAT('%', :search, '%')
            )
              AND (
                :estado IS NULL
                OR (:estado = TRUE AND u.deleted = FALSE)
                OR (:estado = FALSE AND u.deleted = TRUE)
              )
              AND (
                :estadoConexion IS NULL
                OR u.estado_conexion = :estadoConexion
              )
            """, nativeQuery = true)
    Page<UsuarioListProjection> findUsuariosAdmin(
            @Param("search") String search,
            @Param("estado") Boolean estado,
            @Param("estadoConexion") Boolean estadoConexion,
            Pageable pageable);

    @Query(value = """
            SELECT u.id AS id,
                   u.correo AS correo,
                   (NOT u.deleted) AS estado,
                   u.estado_conexion AS estadoConexion,
                   u.ultima_conexion AS ultimaConexion,
                   r.nombre AS rol,
                   u.created_date AS createdDate,
                   u.last_modified_date AS lastModifiedDate,
                   p.nombre AS nombre,
                   p.apellido AS apellido,
                   p.foto_url AS fotoUrl,
                   p.telefono AS telefono,
                   p.descripcion AS descripcion,
                   tp.nombre AS tipoPerfil
            FROM usuario u
            LEFT JOIN perfil p ON p.id_usuario = u.id
            LEFT JOIN tipos_perfil tp ON tp.id = p.id_tipo_perfil
            LEFT JOIN rol r ON r.id = u.id_rol
            WHERE u.id = :id
            """, nativeQuery = true)
    Optional<UsuarioDetailProjection> findDetalleAdminById(@Param("id") UUID id);

    @Query(value = "SELECT COUNT(*) FROM usuario", nativeQuery = true)
    Long countTotalUsuariosIncludingDeleted();

    @Query(value = "SELECT COUNT(*) FROM usuario WHERE deleted = FALSE", nativeQuery = true)
    Long countUsuariosActivos();

    @Query(value = "SELECT COUNT(*) FROM usuario WHERE deleted = TRUE", nativeQuery = true)
    Long countUsuariosInactivos();

    @Modifying
    @Query(value = """
            UPDATE usuario
            SET deleted = TRUE,
                estado_conexion = FALSE,
                last_modified_date = NOW()
            WHERE id = :id
            """, nativeQuery = true)
    int desactivarUsuario(@Param("id") UUID id);

    @Modifying
    @Query(value = """
            UPDATE usuario
            SET deleted = FALSE,
                last_modified_date = NOW()
            WHERE id = :id
            """, nativeQuery = true)
    int activarUsuario(@Param("id") UUID id);

    @Modifying
    @Query(value = """
            UPDATE usuario
            SET correo = :correo,
                last_modified_date = NOW()
            WHERE id = :id
            """, nativeQuery = true)
    int actualizarCorreo(@Param("id") UUID id, @Param("correo") String correo);

    @Modifying
    @Query(value = """
            UPDATE usuario
            SET id_rol = :rolId,
                last_modified_date = NOW()
            WHERE id = :id
            """, nativeQuery = true)
    int actualizarRol(@Param("id") UUID id, @Param("rolId") UUID rolId);

    @Query(value = "SELECT created_date FROM usuario WHERE id = :id", nativeQuery = true)
    Optional<LocalDateTime> findCreatedDateById(@Param("id") UUID id);

    @Query(value = "SELECT deleted FROM usuario WHERE id = :id", nativeQuery = true)
    Optional<Boolean> findDeletedStateById(@Param("id") UUID id);

    @Modifying
    @Query(value = """
            UPDATE usuario
            SET estado_conexion = :estado,
                ultima_conexion = NOW(),
                last_modified_date = NOW()
            WHERE id = :id
            """, nativeQuery = true)
    int actualizarEstadoConexion(@Param("id") UUID id, @Param("estado") boolean estado);

    interface UsuarioListProjection {
        UUID getId();

        String getCorreo();

        String getNombre();

        String getApellido();

        String getTelefono();

        Boolean getEstado();

        Boolean getEstadoConexion();

        String getRol();

        String getTipoPerfil();

        Long getTotalPublicaciones();
    }

    interface UsuarioDetailProjection {
        UUID getId();

        String getCorreo();

        Boolean getEstado();

        Boolean getEstadoConexion();

        LocalDateTime getUltimaConexion();

        String getRol();

        LocalDateTime getCreatedDate();

        LocalDateTime getLastModifiedDate();

        String getNombre();

        String getApellido();

        String getFotoUrl();

        String getTelefono();

        String getDescripcion();

        String getTipoPerfil();
    }
}
