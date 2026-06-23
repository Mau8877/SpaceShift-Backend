package com.sw.api.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseSeeder implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.file:classpath:db/seeders/initial-data.json}")
    private String seedFile;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        Resource resource = resourceLoader.getResource(seedFile);

        if (!resource.exists()) {
            System.out.println("[SEEDER] Archivo seed no encontrado: " + seedFile);
            return;
        }

        SeedData seedData = objectMapper.readValue(resource.getInputStream(), SeedData.class);

        System.out.println("[SEEDER] Iniciando seed de datos iniciales...");

        for (String rol : seedData.getRoles()) {
            findOrCreateRol(rol);
        }

        for (String tipoPerfil : seedData.getTiposPerfil()) {
            findOrCreateTipoPerfil(tipoPerfil);
        }

        for (SeedUser user : seedData.getUsuarios()) {
            seedUsuarioCompleto(user);
        }

        System.out.println("[SEEDER] Seed finalizado correctamente.");
    }

    private void seedUsuarioCompleto(SeedUser user) {
        UUID rolId = findOrCreateRol(user.getRol());
        UUID tipoPerfilId = findOrCreateTipoPerfil(user.getTipoPerfil());

        UUID usuarioId = findUsuarioByCorreo(user.getCorreo())
                .orElseGet(() -> createUsuario(user, rolId));

        updateRolUsuario(usuarioId, rolId);

        upsertPerfil(user, usuarioId, tipoPerfilId);

        if (user.getPublicaciones() != null) {
            for (SeedPublication publicacion : user.getPublicaciones()) {
                seedPublicacion(usuarioId, publicacion);
            }
        }
    }

    private UUID findOrCreateRol(String nombre) {
        return findUuid("SELECT id FROM rol WHERE nombre = ?", nombre)
                .orElseGet(() -> insertUuid(
                        "INSERT INTO rol (nombre) VALUES (?) RETURNING id",
                        nombre));
    }

    private UUID findOrCreateTipoPerfil(String nombre) {
        return findUuid("SELECT id FROM tipos_perfil WHERE nombre = ?", nombre)
                .orElseGet(() -> insertUuid(
                        """
                                INSERT INTO tipos_perfil (nombre)
                                VALUES (?)
                                RETURNING id
                                """,
                        nombre));
    }

    private Optional<UUID> findUsuarioByCorreo(String correo) {
        return findUuid("SELECT id FROM usuario WHERE correo = ?", correo);
    }

    private UUID createUsuario(SeedUser user, UUID rolId) {
        String rawPassword = user.getPassword() != null && !user.getPassword().isBlank()
                ? user.getPassword()
                : "123456";

        String encryptedPassword = passwordEncoder.encode(rawPassword);

        return insertUuid(
                """
                        INSERT INTO usuario (
                            correo,
                            password,
                            estado_conexion,
                            id_rol
                        )
                        VALUES (?, ?, FALSE, ?)
                        RETURNING id
                        """,
                user.getCorreo(),
                encryptedPassword,
                rolId);
    }

    private void updateRolUsuario(UUID usuarioId, UUID rolId) {
        jdbcTemplate.update(
                """
                        UPDATE usuario
                        SET id_rol = ?,
                            last_modified_date = NOW()
                        WHERE id = ?
                        """,
                rolId,
                usuarioId);
    }

    private void upsertPerfil(SeedUser user, UUID usuarioId, UUID tipoPerfilId) {
        jdbcTemplate.queryForObject(
                """
                        INSERT INTO perfil (
                            nombre,
                            apellido,
                            foto_url,
                            telefono,
                            descripcion,
                            id_usuario,
                            id_tipo_perfil
                        )
                        VALUES (?, ?, NULL, ?, ?, ?, ?)
                        ON CONFLICT (id_usuario)
                        DO UPDATE SET
                            nombre = EXCLUDED.nombre,
                            apellido = EXCLUDED.apellido,
                            telefono = EXCLUDED.telefono,
                            descripcion = EXCLUDED.descripcion,
                            id_tipo_perfil = EXCLUDED.id_tipo_perfil,
                            last_modified_date = NOW()
                        RETURNING id
                        """,
                UUID.class,
                user.getNombre(),
                user.getApellido(),
                user.getTelefono(),
                user.getDescripcion(),
                usuarioId,
                tipoPerfilId);
    }

    private void seedPublicacion(UUID usuarioId, SeedPublication publicacion) {
        Optional<SeededPublicationIds> existente = findPublicacionExistente(usuarioId, publicacion.getTitulo());

        if (existente.isPresent()) {
            SeededPublicationIds ids = existente.get();
            updateInmueble(ids.inmuebleId(), publicacion.getInmueble());
            upsertUbicacion(ids.inmuebleId(), publicacion.getUbicacion());
            updatePublicacion(ids.publicacionId(), publicacion);
            upsertImagenPublicacion(ids.publicacionId(), publicacion.getImagenUrl());
            return;
        }

        UUID inmuebleId = createInmueble(publicacion.getInmueble());
        upsertUbicacion(inmuebleId, publicacion.getUbicacion());

        UUID publicacionId = createPublicacion(usuarioId, inmuebleId, publicacion);

        upsertImagenPublicacion(publicacionId, publicacion.getImagenUrl());
    }

    private Optional<SeededPublicationIds> findPublicacionExistente(UUID usuarioId, String titulo) {
        List<SeededPublicationIds> ids = jdbcTemplate.query(
                """
                        SELECT id, id_inmueble
                        FROM publicacion
                        WHERE id_usuario = ?
                          AND titulo = ?
                          AND deleted = FALSE
                        """,
                (rs, rowNum) -> new SeededPublicationIds(
                        (UUID) rs.getObject("id"),
                        (UUID) rs.getObject("id_inmueble")),
                usuarioId,
                titulo);

        return ids.stream().findFirst();
    }

    private UUID createInmueble(SeedInmueble inmueble) {
        String dispJson = null;
        try {
            if (inmueble.getDispositivos() != null) {
                dispJson = objectMapper.writeValueAsString(inmueble.getDispositivos());
            }
        } catch (Exception e) {
            System.err.println("Error serializando dispositivos del seed: " + e.getMessage());
        }

        return insertUuid(
                """
                        INSERT INTO inmueble (
                            tipo_inmueble,
                            area_terreno,
                            area_construida,
                            habitaciones,
                            banos,
                            garajes,
                            antiguedad_anios,
                            dispositivos,
                            condiciones,
                            multas_sanciones
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)
                        RETURNING id
                        """,
                inmueble.getTipoInmueble(),
                inmueble.getAreaTerreno(),
                inmueble.getAreaConstruida(),
                inmueble.getHabitaciones(),
                inmueble.getBanos(),
                inmueble.getGarajes(),
                inmueble.getAntiguedadAnios(),
                dispJson,
                inmueble.getCondiciones(),
                inmueble.getMultasSanciones());
    }

    private void updateInmueble(UUID inmuebleId, SeedInmueble inmueble) {
        String dispJson = null;
        try {
            if (inmueble.getDispositivos() != null) {
                dispJson = objectMapper.writeValueAsString(inmueble.getDispositivos());
            }
        } catch (Exception e) {
            System.err.println("Error serializando dispositivos del seed: " + e.getMessage());
        }

        jdbcTemplate.update(
                """
                        UPDATE inmueble
                        SET tipo_inmueble = ?,
                            area_terreno = ?,
                            area_construida = ?,
                            habitaciones = ?,
                            banos = ?,
                            garajes = ?,
                            antiguedad_anios = ?,
                            dispositivos = ?::jsonb,
                            condiciones = ?,
                            multas_sanciones = ?,
                            last_modified_date = NOW()
                        WHERE id = ?
                        """,
                inmueble.getTipoInmueble(),
                inmueble.getAreaTerreno(),
                inmueble.getAreaConstruida(),
                inmueble.getHabitaciones(),
                inmueble.getBanos(),
                inmueble.getGarajes(),
                inmueble.getAntiguedadAnios(),
                dispJson,
                inmueble.getCondiciones(),
                inmueble.getMultasSanciones(),
                inmuebleId);
    }

    private void upsertUbicacion(UUID inmuebleId, SeedUbicacion ubicacion) {
        jdbcTemplate.update(
                """
                        INSERT INTO ubicacion (
                            id_inmueble,
                            ciudad,
                            zona_barrios,
                            direccion_exacta,
                            latitud,
                            longitud
                        )
                        VALUES (?, ?, ?, ?, ?, ?)
                        ON CONFLICT (id_inmueble)
                        DO UPDATE SET
                            ciudad = EXCLUDED.ciudad,
                            zona_barrios = EXCLUDED.zona_barrios,
                            direccion_exacta = EXCLUDED.direccion_exacta,
                            latitud = EXCLUDED.latitud,
                            longitud = EXCLUDED.longitud,
                            last_modified_date = NOW()
                        """,
                inmuebleId,
                ubicacion.getCiudad(),
                ubicacion.getZonaBarrios(),
                ubicacion.getDireccionExacta(),
                ubicacion.getLatitud(),
                ubicacion.getLongitud());
    }

    private UUID createPublicacion(UUID usuarioId, UUID inmuebleId, SeedPublication publicacion) {
        return insertUuid(
                """
                        INSERT INTO publicacion (
                            id_usuario,
                            id_inmueble,
                            titulo,
                            descripcion_general,
                            tipo_transaccion,
                            precio,
                            moneda,
                            estado_publicacion
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        RETURNING id
                        """,
                usuarioId,
                inmuebleId,
                publicacion.getTitulo(),
                publicacion.getDescripcionGeneral(),
                publicacion.getTipoTransaccion(),
                publicacion.getPrecio(),
                publicacion.getMoneda(),
                publicacion.getEstadoPublicacion());
    }

    private void updatePublicacion(UUID publicacionId, SeedPublication publicacion) {
        jdbcTemplate.update(
                """
                        UPDATE publicacion
                        SET descripcion_general = ?,
                            tipo_transaccion = ?,
                            precio = ?,
                            moneda = ?,
                            estado_publicacion = ?,
                            last_modified_date = NOW()
                        WHERE id = ?
                        """,
                publicacion.getDescripcionGeneral(),
                publicacion.getTipoTransaccion(),
                publicacion.getPrecio(),
                publicacion.getMoneda(),
                publicacion.getEstadoPublicacion(),
                publicacionId);
    }

    private void upsertImagenPublicacion(UUID publicacionId, String imagenUrl) {
        if (imagenUrl == null || imagenUrl.isBlank()) {
            return;
        }

        boolean existePortada = exists(
                """
                        SELECT COUNT(*)
                        FROM imagen_publicacion
                        WHERE id_publicacion = ?
                          AND es_portada = TRUE
                          AND deleted = FALSE
                        """,
                publicacionId);

        if (existePortada) {
            jdbcTemplate.update(
                    """
                            UPDATE imagen_publicacion
                            SET url_image = ?,
                                last_modified_date = NOW()
                            WHERE id_publicacion = ?
                              AND es_portada = TRUE
                              AND deleted = FALSE
                            """,
                    imagenUrl,
                    publicacionId);
            return;
        }

        jdbcTemplate.update(
                """
                        INSERT INTO imagen_publicacion (
                            id_publicacion,
                            url_image,
                            es_portada
                        )
                        VALUES (?, ?, TRUE)
                        """,
                publicacionId,
                imagenUrl);
    }

    private Optional<UUID> findUuid(String sql, Object... params) {
        List<UUID> ids = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> (UUID) rs.getObject("id"),
                params);

        return ids.stream().findFirst();
    }

    private UUID insertUuid(String sql, Object... params) {
        UUID id = jdbcTemplate.queryForObject(sql, UUID.class, params);

        if (id == null) {
            throw new IllegalStateException("No se pudo obtener el UUID insertado.");
        }

        return id;
    }

    private boolean exists(String sql, Object... params) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, params);
        return count != null && count > 0;
    }

    private record SeededPublicationIds(UUID publicacionId, UUID inmuebleId) {
    }

    @Data
    public static class SeedData {
        private List<String> roles = new ArrayList<>();
        private List<String> tiposPerfil = new ArrayList<>();
        private List<SeedUser> usuarios = new ArrayList<>();
    }

    @Data
    public static class SeedUser {
        private String correo;
        private String password;
        private String rol;
        private String tipoPerfil;
        private String nombre;
        private String apellido;
        private String telefono;
        private String descripcion;
        private List<SeedPublication> publicaciones = new ArrayList<>();
    }

    @Data
    public static class SeedPublication {
        private String titulo;
        private String descripcionGeneral;
        private String tipoTransaccion;
        private BigDecimal precio;
        private String moneda;
        private String estadoPublicacion;
        private String imagenUrl;
        private SeedInmueble inmueble;
        private SeedUbicacion ubicacion;
    }

    @Data
    public static class SeedInmueble {
        private String tipoInmueble;
        private BigDecimal areaTerreno;
        private BigDecimal areaConstruida;
        private Integer habitaciones;
        private Integer banos;
        private Integer garajes;
        private Integer antiguedadAnios;
        private Object dispositivos;
        private String condiciones;
        private String multasSanciones;
    }

    @Data
    public static class SeedUbicacion {
        private String ciudad;
        private String zonaBarrios;
        private String direccionExacta;
        private String latitud;
        private String longitud;
    }
}
