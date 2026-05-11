package com.sw.api.modules.reporte.repository;

import com.sw.api.modules.contrato.model.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReporteRepository extends JpaRepository<Contrato, UUID> {

    // -------------------------------------------------------------------------
    // REPORTE USUARIOS
    // -------------------------------------------------------------------------

    @Query(value = """
            SELECT u.id                                        AS id,
                   u.correo                                    AS correo,
                   p.nombre                                    AS nombre,
                   p.apellido                                  AS apellido,
                   p.telefono                                  AS telefono,
                   r.nombre                                    AS rol,
                   CAST(tp.nombre AS TEXT)                     AS tipoPerfil,
                   (NOT u.deleted)                             AS activo,
                   u.estado_conexion                           AS enLinea,
                   u.created_date                              AS fechaRegistro,
                   u.ultima_conexion                           AS ultimaConexion,
                   COALESCE(pub.total, 0)                      AS totalPublicaciones
            FROM usuario u
            LEFT JOIN perfil p        ON p.id_usuario    = u.id
            LEFT JOIN tipos_perfil tp ON tp.id            = p.id_tipo_perfil
            LEFT JOIN rol r           ON r.id             = u.id_rol
            LEFT JOIN (
                SELECT id_usuario, COUNT(*) AS total
                FROM publicacion
                WHERE deleted = FALSE
                GROUP BY id_usuario
            ) pub ON pub.id_usuario = u.id
            WHERE (CAST(:fechaInicio AS TIMESTAMP) IS NULL OR u.created_date >= :fechaInicio)
              AND (CAST(:fechaFin    AS TIMESTAMP) IS NULL OR u.created_date <= :fechaFin)
            ORDER BY u.created_date DESC
            """, nativeQuery = true)
    List<UsuarioReporteProjection> findReporteUsuarios(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);

    // -------------------------------------------------------------------------
    // REPORTE PUBLICACIONES
    // -------------------------------------------------------------------------

    @Query(value = """
            SELECT pub.id                                      AS id,
                   pub.titulo                                  AS titulo,
                   pub.tipo_transaccion                        AS tipoTransaccion,
                   pub.precio                                  AS precio,
                   pub.moneda                                  AS moneda,
                   pub.estado_publicacion                      AS estadoPublicacion,
                   pub.fecha_publicacion                       AS fechaPublicacion,
                   i.tipo_inmueble                             AS tipoInmueble,
                   i.area_terreno                              AS areaTerreno,
                   i.area_construida                           AS areaConstruida,
                   i.habitaciones                              AS habitaciones,
                   i.banos                                     AS banos,
                   i.garajes                                   AS garajes,
                   ub.ciudad                                   AS ciudad,
                   ub.zona_barrios                             AS zonaBarrios,
                   ub.direccion_exacta                         AS direccionExacta,
                   u.correo                                    AS correoPublicador,
                   p.nombre                                    AS nombrePublicador,
                   p.apellido                                  AS apellidoPublicador
            FROM publicacion pub
            JOIN inmueble i         ON i.id           = pub.id_inmueble
            LEFT JOIN ubicacion ub  ON ub.id_inmueble = i.id
            JOIN usuario u          ON u.id           = pub.id_usuario
            LEFT JOIN perfil p      ON p.id_usuario   = u.id
            WHERE pub.deleted = FALSE
              AND (CAST(:fechaInicio AS TIMESTAMP) IS NULL OR pub.fecha_publicacion >= :fechaInicio)
              AND (CAST(:fechaFin    AS TIMESTAMP) IS NULL OR pub.fecha_publicacion <= :fechaFin)
            ORDER BY pub.fecha_publicacion DESC
            """, nativeQuery = true)
    List<PublicacionReporteProjection> findReportePublicaciones(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);

    // -------------------------------------------------------------------------
    // REPORTE CONTRATOS
    // -------------------------------------------------------------------------

    @Query(value = """
            SELECT c.id                                        AS id,
                   c.tipo_contrato                             AS tipoContrato,
                   c.estado_contrato                           AS estadoContrato,
                   c.monto_acordado                            AS montoAcordado,
                   c.moneda                                    AS moneda,
                   c.fecha_inicio                              AS fechaInicio,
                   c.fecha_fin                                 AS fechaFin,
                   c.noches                                    AS noches,
                   c.cantidad_huespedes                        AS cantidadHuespedes,
                   c.created_date                              AS fechaCreacion,
                   prop.correo                                 AS correosPropietario,
                   pp.nombre                                   AS nombrePropietario,
                   cli.correo                                  AS correoCliente,
                   cp.nombre                                   AS nombreCliente,
                   publ.titulo                                 AS tituloPublicacion,
                   inm.tipo_inmueble                           AS tipoInmueble,
                   ub.ciudad                                   AS ciudadInmueble
            FROM contrato c
            JOIN usuario prop        ON prop.id        = c.id_propietario
            LEFT JOIN perfil pp      ON pp.id_usuario  = prop.id
            JOIN usuario cli         ON cli.id         = c.id_cliente
            LEFT JOIN perfil cp      ON cp.id_usuario  = cli.id
            LEFT JOIN publicacion publ ON publ.id      = c.id_publicacion
            JOIN inmueble inm         ON inm.id        = c.id_inmueble
            LEFT JOIN ubicacion ub    ON ub.id_inmueble= inm.id
            WHERE c.deleted = FALSE
              AND (CAST(:fechaInicio AS TIMESTAMP) IS NULL OR c.created_date >= :fechaInicio)
              AND (CAST(:fechaFin    AS TIMESTAMP) IS NULL OR c.created_date <= :fechaFin)
            ORDER BY c.created_date DESC
            """, nativeQuery = true)
    List<ContratoReporteProjection> findReporteContratos(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);

    // -------------------------------------------------------------------------
    // QUERIES ESCALARES PARA RESUMEN
    // -------------------------------------------------------------------------

    @Query(value = "SELECT COUNT(*) FROM usuario", nativeQuery = true)
    Long countTotalUsuarios();

    @Query(value = "SELECT COUNT(*) FROM usuario WHERE deleted = FALSE", nativeQuery = true)
    Long countUsuariosActivos();

    @Query(value = "SELECT COUNT(*) FROM usuario WHERE deleted = TRUE", nativeQuery = true)
    Long countUsuariosInactivos();

    @Query(value = "SELECT COUNT(*) FROM usuario u JOIN rol r ON r.id = u.id_rol WHERE r.nombre = 'ROLE_USER'", nativeQuery = true)
    Long countRoleUser();

    @Query(value = "SELECT COUNT(*) FROM usuario u JOIN rol r ON r.id = u.id_rol WHERE r.nombre = 'ROLE_ADMIN'", nativeQuery = true)
    Long countRoleAdmin();

    @Query(value = "SELECT COUNT(*) FROM publicacion WHERE deleted = FALSE", nativeQuery = true)
    Long countTotalPublicaciones();

    @Query(value = "SELECT COUNT(*) FROM publicacion WHERE deleted = FALSE AND estado_publicacion = 'DISPONIBLE'", nativeQuery = true)
    Long countPublicacionesDisponibles();

    @Query(value = "SELECT COUNT(*) FROM publicacion WHERE deleted = FALSE AND tipo_transaccion = 'VENTA'", nativeQuery = true)
    Long countPublicacionesVenta();

    @Query(value = "SELECT COUNT(*) FROM publicacion WHERE deleted = FALSE AND tipo_transaccion = 'ALQUILER'", nativeQuery = true)
    Long countPublicacionesAlquiler();

    @Query(value = "SELECT COUNT(*) FROM contrato WHERE deleted = FALSE", nativeQuery = true)
    Long countTotalContratos();

    @Query(value = "SELECT COUNT(*) FROM contrato WHERE deleted = FALSE AND estado_contrato = 'ACTIVO'", nativeQuery = true)
    Long countContratosActivos();

    @Query(value = "SELECT COUNT(*) FROM contrato WHERE deleted = FALSE AND estado_contrato = 'CERRADO'", nativeQuery = true)
    Long countContratosCerrados();

    @Query(value = "SELECT COUNT(*) FROM contrato WHERE deleted = FALSE AND estado_contrato = 'CANCELADO'", nativeQuery = true)
    Long countContratosCancelados();

    @Query(value = "SELECT COUNT(*) FROM contrato WHERE deleted = FALSE AND tipo_contrato = 'COMPRAVENTA'", nativeQuery = true)
    Long countContratosCompraventa();

    @Query(value = "SELECT COUNT(*) FROM contrato WHERE deleted = FALSE AND tipo_contrato = 'ALQUILER'", nativeQuery = true)
    Long countContratosAlquiler();

    @Query(value = "SELECT COUNT(*) FROM contrato WHERE deleted = FALSE AND tipo_contrato = 'HOSPEDAJE'", nativeQuery = true)
    Long countContratosHospedaje();

    @Query(value = "SELECT COALESCE(SUM(monto_acordado), 0) FROM contrato WHERE deleted = FALSE", nativeQuery = true)
    BigDecimal sumMontoTotal();

    @Query(value = "SELECT COALESCE(SUM(monto_acordado), 0) FROM contrato WHERE deleted = FALSE AND estado_contrato = 'ACTIVO'", nativeQuery = true)
    BigDecimal sumMontoActivos();

    // -------------------------------------------------------------------------
    // PROYECCIONES
    // -------------------------------------------------------------------------

    interface UsuarioReporteProjection {
        UUID getId();
        String getCorreo();
        String getNombre();
        String getApellido();
        String getTelefono();
        String getRol();
        String getTipoPerfil();
        Boolean getActivo();
        Boolean getEnLinea();
        LocalDateTime getFechaRegistro();
        LocalDateTime getUltimaConexion();
        Long getTotalPublicaciones();
    }

    interface PublicacionReporteProjection {
        UUID getId();
        String getTitulo();
        String getTipoTransaccion();
        BigDecimal getPrecio();
        String getMoneda();
        String getEstadoPublicacion();
        LocalDateTime getFechaPublicacion();
        String getTipoInmueble();
        BigDecimal getAreaTerreno();
        BigDecimal getAreaConstruida();
        Integer getHabitaciones();
        Integer getBanos();
        Integer getGarajes();
        String getCiudad();
        String getZonaBarrios();
        String getDireccionExacta();
        String getCorreoPublicador();
        String getNombrePublicador();
        String getApellidoPublicador();
    }

    interface ContratoReporteProjection {
        UUID getId();
        String getTipoContrato();
        String getEstadoContrato();
        BigDecimal getMontoAcordado();
        String getMoneda();
        LocalDate getFechaInicio();
        LocalDate getFechaFin();
        Integer getNoches();
        Integer getCantidadHuespedes();
        LocalDateTime getFechaCreacion();
        String getCorreosPropietario();
        String getNombrePropietario();
        String getCorreoCliente();
        String getNombreCliente();
        String getTituloPublicacion();
        String getTipoInmueble();
        String getCiudadInmueble();
    }
}
