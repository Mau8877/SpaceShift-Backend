# Sugerencias de Enums — SpaceShift-Backend

Análisis de todos los campos `String` en las entidades JPA que podrían convertirse en enums Java.
Los valores propuestos se basan en los valores encontrados en las migraciones SQL, los defaults del código y la lógica del dominio inmobiliario.

---

## Ya implementados ✅

| Enum | Paquete | Usado en |
|---|---|---|
| `EstadoMensaje` | `modules.chat.model` | `Mensaje.estado` |
| `RolParticipante` | `modules.chat.model` | `ParticipanteConversacion.rol` |
| `PlataformaDispositivo` | `modules.chat.model` | `TokenDispositivo.plataforma` |

---

## Implementado en esta iteración ✅

### `NombreRol` — `modules/usuario/model/`

Evita el string mágico `"ROLE_USER"` en `AuthService`.

```java
public enum NombreRol {
    ROLE_USER,
    ROLE_ADMIN
}
```

**Fuente de verdad:** `V3__crear_rol.sql`
```sql
INSERT INTO rol (nombre) VALUES ('ROLE_USER');
INSERT INTO rol (nombre) VALUES ('ROLE_ADMIN');
```

**Uso en código:** `rolRepository.findByNombre(NombreRol.ROLE_USER.name())`

> Nota: `Rol` sigue siendo una entidad JPA con su tabla en BD — el enum no la reemplaza, solo elimina el string hardcodeado.

---

## Candidatos pendientes de decisión

### 1. `TipoTransaccion` — `modules/publicacion/model/`

Campo afectado: `Publicacion.tipoTransaccion` (`VARCHAR(50) NOT NULL`)

```java
public enum TipoTransaccion {
    VENTA,
    ARRIENDO,           // largo plazo (meses/años)
    ALQUILER_VACACIONAL // por noches / temporada
}
```

Entidad a actualizar:
- `Publicacion.java`: `String tipoTransaccion` → `@Enumerated(EnumType.STRING) TipoTransaccion tipoTransaccion`
- `PublicacionRequestDTO.java`: cambiar tipo del campo
- `PublicacionResponseDTO.java`: cambiar tipo del campo

---

### 2. `EstadoPublicacion` — `modules/publicacion/model/`

Campo afectado: `Publicacion.estadoPublicacion` (`VARCHAR(50) NOT NULL DEFAULT 'ACTIVO'`)

```java
public enum EstadoPublicacion {
    ACTIVO,    // visible públicamente — default en BD
    INACTIVO,  // oculta temporalmente
    PAUSADO,   // el propietario la pausó
    ARCHIVADO  // cerrada, ya no disponible
}
```

Entidad a actualizar:
- `Publicacion.java`: `String estadoPublicacion = "ACTIVO"` → `EstadoPublicacion estadoPublicacion = EstadoPublicacion.ACTIVO`
- `PublicacionRequestDTO.java` y `PublicacionResponseDTO.java`: cambiar tipo del campo

---

### 3. `TipoInmueble` — `modules/inmueble/model/`

Campo afectado: `Inmueble.tipoInmueble` (`VARCHAR(50) NOT NULL`)

```java
public enum TipoInmueble {
    CASA,
    APARTAMENTO,
    OFICINA,
    BODEGA,
    LOTE,
    LOCAL
}
```

Entidad a actualizar:
- `Inmueble.java`: `String tipoInmueble` → `@Enumerated(EnumType.STRING) TipoInmueble tipoInmueble`
- `InmuebleDTO.java` e `InmuebleRequestDTO.java`: cambiar tipo del campo

> Impacto secundario: `ChatService.java` usa `conversacion.getPropiedad().getTipoInmueble()` como título — habría que llamar `.name()` o agregar un método `getLabel()` al enum.

---

### 4. `Moneda` — `shared/model/` (compartido)

Campos afectados:
- `Publicacion.moneda` (`VARCHAR(10) NOT NULL DEFAULT 'USD'`)
- `Contrato.moneda` (`VARCHAR(10) NOT NULL`)

Va en `shared/` porque lo usan dos módulos distintos.

```java
public enum Moneda {
    USD,  // dólar americano — default en BD
    COP,  // peso colombiano
    EUR   // euro
}
```

Entidades a actualizar:
- `Publicacion.java`: `String moneda = "USD"` → `Moneda moneda = Moneda.USD`
- `Contrato.java`: `String moneda` → `Moneda moneda`
- DTOs correspondientes

---

### 5. `TipoContrato` — `modules/contrato/model/`

Campo afectado: `Contrato.tipoContrato` (`VARCHAR(50) NOT NULL`)

```java
public enum TipoContrato {
    COMPRAVENTA,   // venta definitiva
    ARRENDAMIENTO, // arriendo largo plazo
    ALQUILER       // corta temporada
}
```

---

### 6. `EstadoContrato` — `modules/contrato/model/`

Campo afectado: `Contrato.estadoContrato` (`VARCHAR(50) NOT NULL`)

```java
public enum EstadoContrato {
    PENDIENTE,   // creado, sin firmar
    VIGENTE,     // activo y firmado
    FINALIZADO,  // cumplido
    CANCELADO    // cancelado por alguna parte
}
```

---

## Mapa de ubicación final (si se implementan todos)

```
modules/
├── usuario/
│   └── model/
│       └── NombreRol.java          ← implementado ✅
├── inmueble/
│   └── model/
│       └── TipoInmueble.java
├── publicacion/
│   └── model/
│       ├── TipoTransaccion.java
│       └── EstadoPublicacion.java
└── contrato/
    └── model/
        ├── TipoContrato.java
        └── EstadoContrato.java

shared/
└── model/
    └── Moneda.java                  ← compartido por publicacion y contrato
```

---

## Impacto en migraciones SQL

Cuando se adopte un enum en el modelo JPA, Hibernate escribe el `.name()` del enum en la columna.
Los valores ya existentes en BD deben coincidir exactamente con los nombres del enum.

Si la BD ya tiene datos con los strings anteriores (p. ej. `"ACTIVO"`) y el enum se llama `ACTIVO`, no hay problema. Si hubiera discrepancias habría que crear una migración Flyway de corrección antes de arrancar la app.

```sql
-- Ejemplo si el valor viejo fuera "Activo" y el nuevo enum es "ACTIVO"
UPDATE publicacion SET estado_publicacion = 'ACTIVO' WHERE estado_publicacion = 'Activo';
```
