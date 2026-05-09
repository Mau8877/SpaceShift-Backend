# Migración a Arquitectura Modular

## Por qué se hizo este cambio

El proyecto comenzó con una **arquitectura en capas plana**: todos los controllers en una carpeta, todos los services en otra, todos los models en otra, y así sucesivamente. Este enfoque funciona bien cuando el sistema es pequeño, pero a medida que crece se vuelve difícil de navegar porque no hay relación visible entre archivos del mismo dominio.

Por ejemplo, para entender cómo funciona el chat había que abrir `controllers/ChatController.java`, `controllers/ChatWebSocketController.java`, `services/ChatService.java`, `models/Chat/Conversacion.java`, `models/Chat/Mensaje.java`, `repositories/ConversacionRepository.java`, `dtos/ChatDTO.java`... todos en carpetas distintas sin ninguna relación estructural entre sí.

La **arquitectura modular por dominio** agrupa todo lo que pertenece al mismo concepto de negocio en un solo lugar. El resultado es que para entender el chat solo hay que abrir `modules/chat/`.

---

## Qué cambió exactamente

### Estructura anterior

```
com.sw.api/
├── controllers/        ← todos los controllers mezclados
├── services/           ← todos los services mezclados
├── models/             ← todos los modelos mezclados
│   └── Chat/           ← subcarpeta especial para chat
├── repositories/       ← todos los repositories mezclados
└── dtos/               ← todos los DTOs mezclados
```

### Estructura nueva

```
com.sw.api/
├── modules/
│   ├── auth/
│   │   ├── controller/
│   │   ├── service/
│   │   └── dto/
│   ├── usuario/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── model/
│   │   ├── repository/
│   │   └── dto/
│   ├── inmueble/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── model/
│   │   ├── repository/
│   │   └── dto/
│   ├── publicacion/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── model/
│   │   ├── repository/
│   │   └── dto/
│   ├── contrato/
│   │   └── model/
│   └── chat/
│       ├── controller/
│       ├── service/
│       ├── model/
│       ├── repository/
│       └── dto/
├── shared/
│   ├── model/          ← Auditable (base compartida)
│   └── service/        ← CloudinaryService (infraestructura)
├── security/           ← sin cambios
└── config/             ← imports actualizados
```

---

## Inventario de los 65 archivos migrados

### `shared/` — infraestructura transversal

| Archivo original | Archivo nuevo | Cambio en paquete |
|---|---|---|
| `models/Auditable.java` | `shared/model/Auditable.java` | `com.sw.api.models` → `com.sw.api.shared.model` |
| `services/CloudinaryService.java` | `shared/service/CloudinaryService.java` | `com.sw.api.services` → `com.sw.api.shared.service` |

`CloudinaryService` se movió a `shared/` porque es infraestructura pura: no pertenece a ningún dominio específico. Si en el futuro se necesita subir fotos de perfil o documentos de contratos, ningún módulo debería ser dueño de este servicio.

---

### `modules/usuario/` — 12 archivos

| Archivo original | Archivo nuevo |
|---|---|
| `models/Usuario.java` | `modules/usuario/model/Usuario.java` |
| `models/Perfil.java` | `modules/usuario/model/Perfil.java` |
| `models/Rol.java` | `modules/usuario/model/Rol.java` |
| `models/TipoPerfil.java` | `modules/usuario/model/TipoPerfil.java` |
| `repositories/UsuarioRepository.java` | `modules/usuario/repository/UsuarioRepository.java` |
| `repositories/PerfilRepository.java` | `modules/usuario/repository/PerfilRepository.java` |
| `repositories/RolRepository.java` | `modules/usuario/repository/RolRepository.java` |
| `repositories/TipoPerfilRepository.java` | `modules/usuario/repository/TipoPerfilRepository.java` |
| `services/PerfilService.java` | `modules/usuario/service/PerfilService.java` |
| `controllers/PerfilController.java` | `modules/usuario/controller/PerfilController.java` |
| `dtos/PerfilResponseDTO.java` | `modules/usuario/dto/PerfilResponseDTO.java` |
| `dtos/PerfilPatchRequestDTO.java` | `modules/usuario/dto/PerfilPatchRequestDTO.java` |

`Usuario` ahora importa `com.sw.api.shared.model.Auditable` en lugar del antiguo `com.sw.api.models.Auditable`.

---

### `modules/auth/` — 6 archivos

| Archivo original | Archivo nuevo |
|---|---|
| `controllers/AuthController.java` | `modules/auth/controller/AuthController.java` |
| `services/AuthService.java` | `modules/auth/service/AuthService.java` |
| `dtos/LoginRequest.java` | `modules/auth/dto/LoginRequest.java` |
| `dtos/RegisterRequest.java` | `modules/auth/dto/RegisterRequest.java` |
| `dtos/AuthResponse.java` | `modules/auth/dto/AuthResponse.java` |
| `dtos/RefreshTokenRequest.java` | `modules/auth/dto/RefreshTokenRequest.java` |

`AuthService` importa de `modules.usuario.*` (modelos y repositorios) y de `security.JwtService`.

---

### `modules/inmueble/` — 9 archivos

| Archivo original | Archivo nuevo |
|---|---|
| `models/Inmueble.java` | `modules/inmueble/model/Inmueble.java` |
| `models/Ubicacion.java` | `modules/inmueble/model/Ubicacion.java` |
| `repositories/InmuebleRepository.java` | `modules/inmueble/repository/InmuebleRepository.java` |
| `repositories/UbicacionRepository.java` | `modules/inmueble/repository/UbicacionRepository.java` |
| `services/InmuebleService.java` | `modules/inmueble/service/InmuebleService.java` |
| `controllers/InmuebleController.java` | `modules/inmueble/controller/InmuebleController.java` |
| `dtos/InmuebleDTO.java` | `modules/inmueble/dto/InmuebleDTO.java` |
| `dtos/InmuebleRequestDTO.java` | `modules/inmueble/dto/InmuebleRequestDTO.java` |
| `dtos/UbicacionDTO.java` | `modules/inmueble/dto/UbicacionDTO.java` |

`Inmueble` y `Ubicacion` extienden `Auditable` desde `com.sw.api.shared.model`.

---

### `modules/publicacion/` — 10 archivos

| Archivo original | Archivo nuevo |
|---|---|
| `models/Publicacion.java` | `modules/publicacion/model/Publicacion.java` |
| `models/ImagenPublicacion.java` | `modules/publicacion/model/ImagenPublicacion.java` |
| `repositories/PublicacionRepository.java` | `modules/publicacion/repository/PublicacionRepository.java` |
| `repositories/ImagenPublicacionRepository.java` | `modules/publicacion/repository/ImagenPublicacionRepository.java` |
| `services/PublicacionService.java` | `modules/publicacion/service/PublicacionService.java` |
| `controllers/PublicacionController.java` | `modules/publicacion/controller/PublicacionController.java` |
| `controllers/UploadController.java` | `modules/publicacion/controller/UploadController.java` |
| `dtos/PublicacionRequestDTO.java` | `modules/publicacion/dto/PublicacionRequestDTO.java` |
| `dtos/PublicacionResponseDTO.java` | `modules/publicacion/dto/PublicacionResponseDTO.java` |
| `dtos/ImagenPublicacionDTO.java` | `modules/publicacion/dto/ImagenPublicacionDTO.java` |

`UploadController` se movió aquí (no a `shared/`) porque su único propósito es subir imágenes de publicaciones a la carpeta `spaceshift_inmuebles` de Cloudinary. Aunque usa `CloudinaryService`, es un endpoint de dominio de publicaciones.

`PublicacionResponseDTO` importa `InmuebleDTO` desde `com.sw.api.modules.inmueble.dto`.

---

### `modules/contrato/` — 1 archivo (stub)

| Archivo original | Archivo nuevo |
|---|---|
| `models/Contrato.java` | `modules/contrato/model/Contrato.java` |

`Contrato` relaciona `Inmueble + Publicacion + Usuario (propietario) + Usuario (cliente)`. El módulo existe como placeholder: cuando se implementen el service y controller irán en `modules/contrato/service/` y `modules/contrato/controller/`.

---

### `modules/chat/` — 22 archivos

#### Modelos (8)

| Archivo original | Archivo nuevo |
|---|---|
| `models/Chat/Conversacion.java` | `modules/chat/model/Conversacion.java` |
| `models/Chat/Mensaje.java` | `modules/chat/model/Mensaje.java` |
| `models/Chat/ParticipanteConversacion.java` | `modules/chat/model/ParticipanteConversacion.java` |
| `models/Chat/ParticipanteConversacionId.java` | `modules/chat/model/ParticipanteConversacionId.java` |
| `models/Chat/EstadoMensaje.java` | `modules/chat/model/EstadoMensaje.java` |
| `models/Chat/RolParticipante.java` | `modules/chat/model/RolParticipante.java` |
| `models/Chat/TokenDispositivo.java` | `modules/chat/model/TokenDispositivo.java` |
| `models/Chat/PlataformaDispositivo.java` | `modules/chat/model/PlataformaDispositivo.java` |

#### Repositories (4)

| Archivo original | Archivo nuevo |
|---|---|
| `repositories/ConversacionRepository.java` | `modules/chat/repository/ConversacionRepository.java` |
| `repositories/MensajeRepository.java` | `modules/chat/repository/MensajeRepository.java` |
| `repositories/ParticipanteConversacionRepository.java` | `modules/chat/repository/ParticipanteConversacionRepository.java` |
| `repositories/TokenDispositivoRepository.java` | `modules/chat/repository/TokenDispositivoRepository.java` |

#### DTOs (6)

| Archivo original | Archivo nuevo |
|---|---|
| `dtos/ChatDTO.java` | `modules/chat/dto/ChatDTO.java` |
| `dtos/MensajeDTO.java` | `modules/chat/dto/MensajeDTO.java` |
| `dtos/CrearChatRequest.java` | `modules/chat/dto/CrearChatRequest.java` |
| `dtos/SendMessageRequest.java` | `modules/chat/dto/SendMessageRequest.java` |
| `dtos/TypingEvent.java` | `modules/chat/dto/TypingEvent.java` |
| `dtos/TypingRequest.java` | `modules/chat/dto/TypingRequest.java` |

#### Service y Controllers (3)

| Archivo original | Archivo nuevo |
|---|---|
| `services/ChatService.java` | `modules/chat/service/ChatService.java` |
| `controllers/ChatController.java` | `modules/chat/controller/ChatController.java` |
| `controllers/ChatWebSocketController.java` | `modules/chat/controller/ChatWebSocketController.java` |

`ChatService` usaba referencias de clase completa como `com.sw.api.models.Perfil` en el cuerpo del método. En la versión nueva importa `Perfil` limpiamente desde `com.sw.api.modules.usuario.model.Perfil`.

---

### `config/` — 2 archivos actualizados (sin mover)

Estos archivos permanecen en `config/` pero sus imports fueron corregidos:

| Archivo | Import anterior | Import nuevo |
|---|---|---|
| `ApplicationConfig.java` | `com.sw.api.repositories.UsuarioRepository` | `com.sw.api.modules.usuario.repository.UsuarioRepository` |
| `AuditConfig.java` | `com.sw.api.models.Usuario` | `com.sw.api.modules.usuario.model.Usuario` |

---

## Regla de dependencias entre módulos

Las dependencias entre módulos siguen una dirección única. Ningún módulo de nivel inferior puede importar de uno superior.

```
shared           ← no importa nada del dominio

usuario          → shared (Auditable)

inmueble         → shared (Auditable)

publicacion      → inmueble (Inmueble, InmuebleService, InmuebleDTO)
                 → usuario (Usuario, UsuarioRepository)
                 → shared (Auditable, CloudinaryService)

auth             → usuario (todos los modelos y repos)
                 → security (JwtService)

contrato         → inmueble (Inmueble)
                 → publicacion (Publicacion)
                 → usuario (Usuario)

chat             → inmueble (Inmueble)
                 → publicacion (Publicacion, PublicacionRepository)
                 → usuario (Usuario, Perfil, PerfilRepository)

config           → usuario (UsuarioRepository, Usuario)
```

Ningún módulo importa de `security` excepto `auth`. `shared` no importa de ningún módulo.

---

## Archivos que no se movieron

Los siguientes 9 archivos permanecen exactamente donde estaban. Solo se actualizaron sus imports internos cuando fue necesario:

- `ApiApplication.java`
- `security/JwtService.java`
- `security/JwtAuthenticationFilter.java`
- `security/SecurityConfig.java`
- `security/CorsConfig.java`
- `config/CloudinaryConfig.java`
- `config/WebSocketConfig.java`
- `config/ApplicationConfig.java` *(import actualizado)*
- `config/AuditConfig.java` *(import actualizado)*

---

## Verificación post-migración

```bash
# Compilar desde cero
./mvnw clean compile

# Confirmar que no quedan imports de los paquetes viejos
grep -r "import com.sw.api.controllers" src/
grep -r "import com.sw.api.services" src/
grep -r "import com.sw.api.models" src/
grep -r "import com.sw.api.repositories" src/
grep -r "import com.sw.api.dtos" src/
# Todos deben devolver 0 resultados
```

Resultado obtenido: **BUILD SUCCESS** — 70 archivos compilados en ~5.5 segundos, 0 errores.
