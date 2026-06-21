# Documentación de Endpoints: Procesamiento 3D (S3 + Runpod)

Este documento detalla el flujo de integración desde la subida de un video (Frontend) hasta la generación del modelo 3D (Backend + Runpod). Está diseñado para ser consumido en 3 simples pasos.

---

## Flujo de Trabajo

### Paso 0 (Opcional): Cotizar el procesamiento
**`GET /api/videos/cotizar?duracionSegundos=15`**

Antes de procesar, el frontend puede consultar cuánto costará y si el usuario tiene saldo suficiente. **No debita créditos.** Requiere token JWT.

**Respuesta Exitosa (200 OK):**
```json
{
  "duracionSegundos": 15,
  "factorPorSegundo": 2,
  "costoCreditos": 30,
  "saldoActual": 1000,
  "saldoSuficiente": true
}
```

> **Acción del Frontend:** Mostrar `costoCreditos` al usuario. Si acepta y `saldoSuficiente` es `true`, continuar con el Paso 1 → 2.

---

### Paso 1: Solicitar URL Pre-firmada de S3
**`GET /api/videos/upload-url`**

Este endpoint permite solicitar una URL de Amazon S3 que autoriza al frontend a subir un archivo directamente al bucket sin sobrecargar el servidor backend.

**Parámetros:**
- `extension` (Query Param, Opcional): La extensión del archivo (Ej. `.mp4`). Por defecto es `.mp4`.

**Respuesta Exitosa (200 OK):**
```json
{
  "uploadUrl": "https://workflowdesigner-016696895571-us-east-1-an.s3.us-east-1.amazonaws.com/d9a1b2c3-4d5e-6f7a-8b9c-0d1e2f3a4b5c.mp4?X-Amz-Algorithm=...",
  "key": "d9a1b2c3-4d5e-6f7a-8b9c-0d1e2f3a4b5c.mp4"
}
```

> **Acción del Frontend:** Una vez recibida la respuesta, el frontend debe realizar una petición `PUT` directamente a la URL indicada en `uploadUrl` mandando el archivo binario del video. Luego, guardar el valor de `key` para el siguiente paso.

---

### Paso 2: Registrar Video e Iniciar Procesamiento
**`POST /api/videos/publicaciones/{idPublicacion}`**

Este endpoint se invoca **después** de que el video se haya terminado de subir con éxito a S3. Vincula el video subido con una Publicación, debita los tokens del usuario e inicia asíncronamente el trabajo en Runpod.

**Autenticación requerida:** Token JWT válido del usuario.

**Parámetros en Ruta (Path Variable):**
- `idPublicacion`: UUID de la publicación a la que pertenece el video.

**Cuerpo de la Petición (Request Body):**
```json
{
  "keyS3": "d9a1b2c3-4d5e-6f7a-8b9c-0d1e2f3a4b5c.mp4", // El "key" del Paso 1
  "nombreArchivo": "mi_video_final.mp4", // Nombre descriptivo u original
  "tamanoBytes": 15485760, // Tamaño del archivo en bytes
  "duracionSegundos": 15 // Duración del video (Define el cobro: 2 tokens por seg)
}
```

**Respuesta Exitosa (201 Created):**
```json
{
  "id": "11111111-2222-3333-4444-555555555555", // Este es el ID_VIDEO para el Paso 3
  "idPublicacion": "...",
  "urlVideo": "d9a1b2c3-4d5e-6f7a-8b9c-0d1e2f3a4b5c.mp4",
  "duracionSegundos": 15,
  "creditosConsumidos": 30, // 15 seg * 2 tokens
  "estadoProcesamiento": "PROCESANDO",
  "nombreArchivo": "mi_video_final.mp4",
  "tamanoBytes": 15485760
}
```

---

### Paso 3: Consultar Estado (Polling)
**`GET /api/videos/{idVideo}/estado`**

Debido a que la reconstrucción 3D puede tardar varios minutos, el frontend debe consultar periódicamente (Ej. cada 5-10 segundos) este endpoint. Nuestro backend se encargará de hacer la consulta real hacia los servidores de Runpod.

**Parámetros en Ruta (Path Variable):**
- `idVideo`: El UUID del video devuelto en el Paso 2 (`id`).

**Posibles Respuestas (200 OK):**

**A. Sigue Procesando:**
```json
{
  "id": "11111111...",
  "estadoProcesamiento": "PROCESANDO",
  ...
}
```

**B. Terminado con Éxito (Modelo 3D Generado):**
En cuanto pase a estado `COMPLETADO`, las URLs de los recursos 3D (Splat, JSON y la previsualización) se mostrarán pobladas.
```json
{
  "id": "11111111...",
  "estadoProcesamiento": "COMPLETADO",
  "urlSplat": "https://..._u2.splat",
  "urlSog": "https://..._u2.sog",
  "urlJsonModelo": "https://..._meta.json",
  "urlPreviewWebp": "https://..._preview.webp",
  ...
}
```

> El modelo viene en `output.assets.model` de Runpod. Según su extensión se guarda en `urlSplat` (`.splat`) o en `urlSog` (`.sog`); solo uno estará poblado por video. `urlModelo3D` mantiene la URL del modelo por compatibilidad.

**C. Fallo en el Servidor / Video Inválido:**
Si Runpod falla (movimiento brusco, video muy oscuro), el estado pasará a `FALLIDO`. Los tokens cobrados **ya habrán sido reembolsados automáticamente al usuario**.
```json
{
  "id": "11111111...",
  "estadoProcesamiento": "FALLIDO",
  "errorMensaje": "La reconstrucción 3D en Runpod falló."
}
```

---

## Endpoint Secundario: Listar todos los videos de una publicación
**`GET /api/videos/publicaciones/{idPublicacion}`**

Devuelve un Array/Lista con todos los videos vinculados a una publicación, su estado actual y sus respectivos modelos 3D si ya finalizaron exitosamente.

**Respuesta Exitosa (200 OK):**
```json
[
  {
    "id": "11111111...",
    "estadoProcesamiento": "COMPLETADO",
    "urlSplat": "...",
    ...
  },
  {
    "id": "22222222...",
    "estadoProcesamiento": "PROCESANDO",
    "urlSplat": null,
    ...
  }
]
```
