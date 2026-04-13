# Guía de Implementación para Notificaciones Push (Fase Futura)

Esta guía documenta los pasos necesarios para agregar la lógica de **Notificaciones Push y Estados Efímeros** al módulo de chat de "SpaceShift AR". Este trabajo requiere integración con **Redis** y **Firebase Admin SDK** y ha sido delegado como una iteración independiente.

## 1. Prerrequisitos de Tecnologías

Antes de empezar, el equipo debe agregar las dependencias correspondientes en `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>com.google.firebase</groupId>
    <artifactId>firebase-admin</artifactId>
    <version>9.2.0</version>
</dependency>
```

## 2. Gestión de Estados Efímeros en Redis (En Línea / Desconectado)

1. **Configurar Redis:** Asegúrate de tener una instancia de Redis corriendo (puede ser vía Docker).
2. **Interceptar Conexión STOMP:** En el paquete `config`, crea una clase `WebSocketEventListener`.
   - Utiliza `@EventListener` para capturar `SessionConnectEvent` y `SessionDisconnectEvent`.
   - Cuando un evento de conexión se dispare, guarda en Redis una llave clave-valor: `user:{id_del_usuario}:status = "online"` con un tiempo de expiración (TTL).
   - Opcionalmente, intercepta un evento de "escribiendo" mediante `@MessageMapping("/chat.typing")` y retransmite a la sala.

## 3. Integración con Firebase Admin SDK

1. **Credenciales Seguras:** Obtén el archivo `.json` de credenciales de servicio desde la consola de Firebase. Guárdalo de manera segura (idealmente en las variables de entorno de AWS/GCP o fuera del repositorio de código) y configúralo en la app de Spring Boot (clase `@Configuration FirebaseConfig`).
2. **Endpoint de Sincronización:** Habilita el endpoint `POST /api/users/fcm-token` para recibir el token generado por [Flutter / Firebase SDK] en los dispositivos cliente y guárdalo en la tabla `token_dispositivo`.

## 4. Orquestación del Envío de Mensajes (Doble Check)

Modificar la clase actual `ChatWebSocketController` en el método `sendMessage` con el siguiente flujo:
1. Extraer el receptor del chat.
2. Leer desde Redis si la clave `user:{uuid_otro}:status` existe y vale `"online"`.
3. **Si ESTÁ en línea:** Se emite normal por STOMP (`SimpMessagingTemplate`).
4. **Si NO ESTÁ en línea:**
   - Buscar su FCM Token desde el repositorio `tokenDispositivoRepository`.
   - Disparar la notificación Push usando `FirebaseMessaging.getInstance().send(message)`.
   - El payload del Firebase Message debe incluir los `data` attributes que requiera Flutter para abrir el Chat internamente.
