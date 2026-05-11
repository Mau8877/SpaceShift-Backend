# Guía: Servicio de Notificaciones Push

Esta guía explica cómo enviar notificaciones push desde cualquier parte del backend usando `NotificacionService`.

---

## ¿Cómo funciona?

El flujo completo es:

```
Tu Service/Controller
  └─ NotificacionService.enviarNotificacion(...)
       └─ Busca tokens FCM del usuario en BD (token_dispositivo)
            └─ Llama Firebase Admin SDK → FCM → dispositivo del usuario
```

El servicio maneja múltiples dispositivos por usuario (Android e iOS) y limpia automáticamente tokens inválidos de la BD.

---

## Inyección del servicio

Inyecta `NotificacionService` con `@RequiredArgsConstructor` en cualquier `@Service` o `@Controller`:

```java
@Service
@RequiredArgsConstructor
public class TuServicio {

    private final NotificacionService notificacionService;

    // ...
}
```

---

## Método principal

```java
notificacionService.enviarNotificacion(
    UUID usuarioId,      // ID del usuario que RECIBE la notificación
    String titulo,       // Título que aparece en la notificación
    String cuerpo,       // Texto del cuerpo
    Map<String, String> data  // Datos extra para que Flutter navegue al destino correcto
);
```

> **Nota:** Si el usuario no tiene ningún dispositivo registrado (por ejemplo, solo usa la web), el método retorna sin error.

---

## Campo `data` — cómo Flutter lo usa

El mapa `data` es lo que permite a la app mobile navegar a la pantalla correcta al tocar la notificación.

| Campo | Obligatorio | Descripción |
|-------|-------------|-------------|
| `type` | Sí | Identifica el tipo de evento. Flutter decide a qué pantalla ir. |
| Campos adicionales | Depende del `type` | IDs necesarios para navegar al recurso. |

### Tipos definidos actualmente en Flutter

| `type` | Campos extra | Pantalla destino |
|--------|-------------|-----------------|
| `NEW_MESSAGE` | `conversacionId` | `/chat_detail/{conversacionId}` |

Para agregar nuevos tipos, añade un `else if` en `NotificationService._navigate()` en Flutter.

---

## Ejemplos de uso

### 1. Nuevo mensaje de chat (ya implementado)

```java
notificacionService.enviarNotificacion(
    destinatario.getId(),
    "Nuevo mensaje de " + nombreRemitente,
    contenido,
    Map.of(
        "type", "NEW_MESSAGE",
        "conversacionId", conversacionId.toString()
    )
);
```

---

### 2. Reserva / contrato confirmado

```java
// En ContratoService o donde confirmes la reserva:
notificacionService.enviarNotificacion(
    clienteId,
    "¡Reserva confirmada!",
    "Tu reserva para \"" + tituloPropiedad + "\" fue aprobada.",
    Map.of(
        "type", "BOOKING_CONFIRMED",
        "contratoId", contrato.getId().toString()
    )
);
```

---

### 3. Publicación actualizada (notificar al propietario)

```java
// En PublicacionService cuando un admin actualiza el estado:
notificacionService.enviarNotificacion(
    publicacion.getUsuario().getId(),
    "Tu publicación fue actualizada",
    "\"" + publicacion.getTitulo() + "\" cambió a estado: " + nuevoEstado,
    Map.of(
        "type", "PUBLICATION_UPDATE",
        "publicacionId", publicacion.getId().toString()
    )
);
```

---

### 4. Nuevo interesado en una propiedad

```java
// En ChatService cuando se crea un nuevo chat sobre una publicación:
notificacionService.enviarNotificacion(
    propietarioId,
    "Nuevo interesado en tu propiedad",
    nombreCliente + " quiere saber más sobre \"" + tituloPropiedad + "\".",
    Map.of(
        "type", "NEW_INQUIRY",
        "conversacionId", conversacion.getId().toString()
    )
);
```

---

### 5. Registro de token (desde el controller — ya implementado)

```java
// POST /api/notificaciones/token — llamado automáticamente desde Flutter al hacer login
notificacionService.registrarToken(usuario, tokenFcm, PlataformaDispositivo.ANDROID);
```

---

## Registro de tokens REST

| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| `POST` | `/api/notificaciones/token` | Registra o actualiza el token FCM del usuario autenticado | JWT requerido |
| `DELETE` | `/api/notificaciones/token` | Revoca el token al hacer logout | JWT requerido |

**Body (ambos endpoints):**
```json
{
  "tokenFcm": "token_fcm_del_dispositivo",
  "plataforma": "ANDROID"
}
```

Valores válidos para `plataforma`: `ANDROID`, `IOS`, `WEB`

---

## Comportamiento ante errores de FCM

| Situación | Comportamiento |
|-----------|---------------|
| Usuario sin dispositivos registrados | Se omite silenciosamente, sin excepción |
| Token expirado o inválido (`UNREGISTERED`) | Se elimina automáticamente de la BD |
| Error de red con Firebase | Se loguea la excepción, no interrumpe el flujo de negocio |
| Firebase no inicializado | Lanza `IOException` al arrancar la app — revisar `firebase-service-account.json` |

---

## Agregar un nuevo tipo de notificación

### Paso 1 — Backend: úsalo directamente

No hace falta modificar nada en el backend. Solo elige un nombre para `type` y llama al servicio:

```java
notificacionService.enviarNotificacion(
    usuarioId,
    "Título",
    "Cuerpo",
    Map.of("type", "MI_NUEVO_TIPO", "entidadId", id.toString())
);
```

### Paso 2 — Flutter: registrar la navegación

En `lib/core/services/notification_service.dart`, dentro del método `_navigate`:

```dart
void _navigate(RemoteMessage message) {
    final type = message.data['type'];
    if (type == 'NEW_MESSAGE') {
      _ref.read(appRouterProvider).push('/chat_detail/${message.data['conversacionId']}');
    } else if (type == 'MI_NUEVO_TIPO') {
      final id = message.data['entidadId'];
      _ref.read(appRouterProvider).push('/mi_pantalla/$id');
    }
}
```
