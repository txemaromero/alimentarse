# API REST

Todas las rutas cuelgan de `/api`. En el perfil `dev` la documentación interactiva está en **Swagger UI**: `http://localhost:8080/api/docs` (la especificación OpenAPI está en `/api/docs/openapi`). Para usarla, inicia sesión con `POST /api/auth/login`: las lecturas ya funcionan con la cookie de sesión. Para escribir, copia el valor de la cookie `toca_sesion` (herramientas de desarrollo del navegador → Aplicación → Cookies) y pégalo en «Authorize».

La descripción OpenAPI también está en el repositorio, en [`openapi.json`](openapi.json). De ella se generan los tipos del frontend, y los tests comprueban que coincide con la API (D-073). En ella, todos los campos de las respuestas están siempre presentes; los que pueden valer `null` lo indican (`"type": ["string", "null"]`).

## Resumen

| Método y ruta | Acceso | Para qué |
|---|---|---|
| `POST /api/auth/registro` | pública | Alta de usuario |
| `POST /api/auth/login` · `POST /api/auth/logout` | pública | Inicio de sesión (el JWT va en una cookie) y cierre |
| `POST /api/auth/password/olvido` · `POST /api/auth/password/restablecer` | pública | Recuperación de contraseña |
| `POST /api/auth/email/confirmar` · `POST /api/usuarios/me/email/confirmacion` · `DELETE /api/usuarios/me/email/pendiente` | pública; sesión | Confirmación del email (D-088) |
| `GET /api/auth/proveedores` · `GET /api/auth/sesion` · `GET` · `POST /api/auth/google/vincular` · `GET` · `POST /api/auth/google/registro` | pública | Acceso con Google (D-085, D-086) |
| `GET` · `PUT /api/usuarios/me` · `PUT /api/usuarios/me/password` · `POST /api/usuarios/me/desvincular-google` | sesión | Perfil, cambio de contraseña y dejar de entrar con Google |
| `GET /api/usuarios/me/datos` · `POST /api/usuarios/me/baja` | sesión | Descargar mis datos y darse de baja |
| `GET /api/grupos` · `GET /api/tipos-receta` | sesión | Catálogos |
| `GET /api/ingredientes` · `GET /api/ingredientes/sugerencias` · `POST /api/ingredientes` | sesión | Ingredientes: listado, autocompletado y alta |
| `PUT /api/ingredientes/{id}` | ADMIN | Editar un ingrediente |
| `GET /api/recetas` · `/mias` · `/me-gustan` · `/mejor-valoradas` · `/ultimas` · `/sugerencias` · `/{id}` | sesión | Listados, búsqueda y detalle de recetas |
| `POST /api/recetas` · `PUT /api/recetas/{id}` | sesión; editar, solo el autor | Alta y edición (multipart) |
| `PATCH /api/recetas/{id}/publicada` | el autor | Publicar o despublicar |
| `DELETE /api/recetas/{id}` | el autor | Eliminar una receta |
| `POST` · `DELETE /api/recetas/{id}/like` | sesión | «Me gusta» |
| `GET /api/recetas/{id}/valoraciones` · `PUT` · `DELETE /api/recetas/{id}/valoracion` | sesión | Valoraciones: lista y la propia |
| `GET /api/recetas/recomendadas` | sesión | «¿Qué toca hoy?» |
| `GET /api/recetas/{id}/impacto` | sesión | Cómo quedarían las raciones si se come la receta |
| `POST` · `GET /api/consumos` · `PUT` · `DELETE /api/consumos/{id}` | sesión (los propios) | Consumos e histórico |
| `GET /api/alimentacion/resumen` | sesión | «Mi alimentación» |
| `GET /api/alimentacion/nutricion` | sesión | Nutrientes de lo comido en los últimos 7 días (D-082) |
| `GET` · `POST /api/plan` · `DELETE /api/plan/{id}` · `POST /api/plan/{id}/consumir` | sesión (los propios) | Planificador semanal |
| `GET /api/plan/compra` | sesión (los propios) | Lista de la compra de lo planificado |
| `GET /api/push/clave` · `PUT` · `DELETE /api/push/suscripcion` · `POST /api/push/prueba` | sesión | Avisos push del planificador |
| `GET /api/admin/usuarios` · `PUT /api/admin/usuarios/{id}/admin` · `PUT /api/admin/usuarios/{id}/bloqueado` | ADMIN | Gestión de usuarios y bloqueos |
| `GET /api/admin/recetas/ocultas` · `PUT /api/admin/recetas/{id}/oculta` | ADMIN | Moderación de recetas |
| `PUT /api/admin/valoraciones/{id}/oculta` | ADMIN | Moderación de valoraciones |
| `GET` · `POST /api/admin/grupos` · `PUT` · `DELETE /api/admin/grupos/{id}` | ADMIN | Grupos de alimentos y sus raciones |
| `POST /api/admin/ingredientes/{id}/fusionar` · `DELETE /api/admin/ingredientes/{id}` | ADMIN | Fusionar y borrar ingredientes |
| `GET /api/admin/alimentos-bedca` · `GET`/`PUT /api/admin/ingredientes/{id}/nutricion` | ADMIN | Alimento de BEDCA y pesos de las piezas de un ingrediente (D-080, D-081) |
| `GET /api/imagenes/{nombre}` | pública | Imágenes de las recetas |
| `GET /api/health` · `/api/health/liveness` · `/api/health/readiness` | pública | Salud de la aplicación |

## Convenciones

### Autenticación
- **Cookie `toca_sesion`** con el JWT: la pone el login, `HttpOnly`, `SameSite=Strict` y `Path=/api` (D-070).
- **Protección CSRF:** las escrituras (`POST`, `PUT`, `PATCH`, `DELETE`) que se autentican con la cookie deben llevar la cabecera `X-XSRF-TOKEN` con el valor de la cookie `XSRF-TOKEN`; si no, **403**. El `HttpClient` de Angular lo hace solo. No se exige en `/api/auth/**`.
- También se acepta el mismo JWT en la cabecera `Authorization: Bearer <JWT>`, sin CSRF, para llamadas desde fuera del navegador o desde Swagger UI. Otra cabecera `Authorization` (por ejemplo, `Basic`) no evita el CSRF de la cookie (D-086).
- El token caduca a las `JWT_EXPIRATION_HOURS` horas (8 por defecto) y lleva el id, el nombre, los roles del usuario, su versión de sesión y el destinatario `sesion`: los tokens del acceso con Google llevan otro y nunca valen como sesión (D-086).
- **Límites por IP** (429 `DEMASIADOS_INTENTOS`, D-086). Cada grupo tiene su contador, y las rutas se comparan decodificadas (`/api/auth/%6Cogin` cuenta como el login):

  | Peticiones | Límite por defecto |
  |---|---|
  | `POST /api/auth/login` | 10 por minuto |
  | Las que comprueban la contraseña: `POST /api/auth/google/vincular`, `PUT /api/usuarios/me`, `PUT /api/usuarios/me/password`, `POST /api/usuarios/me/desvincular-google`, `POST /api/usuarios/me/baja` | 10 por minuto |
  | `POST /api/auth/password/olvido` | 5 cada 15 minutos |
  | `POST /api/usuarios/me/email/confirmacion` | 5 cada 15 minutos |
  | `POST /api/auth/registro` · `POST /api/auth/google/registro` | 10 por hora |
  | `GET /oauth2/authorization/google` | 20 por minuto |
- **Límite por usuario en el login** (429 `DEMASIADOS_INTENTOS`, D-090): 20 contraseñas fallidas cada 15 minutos con el mismo usuario o email, desde cualquier IP. Solo cuentan los fallos, y el usuario y el email llevan contadores distintos. Restablecer la contraseña con el enlace del correo pone los dos a cero.
- Rutas públicas:
  - `/api/auth/**`
  - `GET /api/imagenes/{nombre}`
  - `GET /api/health/**` (salud)
  - la documentación, solo en `dev`
- `/api/admin/**` exige el rol `ADMIN`.
- El resto de `/api/**` exige sesión.
- En cada petición con token se comprueba que **la cuenta sigue existiendo y no está bloqueada** (D-063). Si no, responde 401 como si no hubiera token; si está bloqueada, el código es `CUENTA_BLOQUEADA`.

### Errores
Todas las respuestas de error tienen el mismo formato. `errores` lleva un mensaje por campo del formulario y va vacío cuando no aplica:

```json
{ "codigo": "VALIDACION", "mensaje": "Revisa los datos del formulario.", "errores": { "email": "El email no es válido" } }
```

| HTTP | `codigo` | Cuándo |
|---|---|---|
| 400 | `VALIDACION` | Algún campo no cumple las reglas (ver `errores`) |
| 400 | `PETICION_INCORRECTA` | El cuerpo no es JSON válido, o falta un parámetro o no tiene el tipo esperado |
| 400 | `TOKEN_INVALIDO` | Enlace de recuperación caducado, ya usado o inexistente |
| 400 | `PASSWORD_INCORRECTA` | La contraseña actual no coincide (`errores.actual`) |
| 401 | `NO_AUTENTICADO` | Falta el token o no es válido o ha caducado |
| 401 | `CREDENCIALES_INVALIDAS` | Login fallido |
| 403 | `ACCESO_DENEGADO` | El usuario no tiene permiso |
| 404 | `NO_ENCONTRADO` | El recurso o la ruta no existen |
| 405 | `METODO_NO_PERMITIDO` | Método HTTP no admitido en esa ruta |
| 409 | `CONFLICTO` | Dato único ya usado (ver `errores`) |
| 429 | `DEMASIADOS_INTENTOS` | Límite de intentos superado; la cabecera `Retry-After` indica los segundos de espera |
| 500 | `ERROR_INTERNO` | Error inesperado; el detalle queda solo en el log del servidor |

Otra petición que Spring MVC rechaza sin un caso propio en esta tabla conserva su estado 4xx, con `PETICION_INCORRECTA`. Si la cabecera `Accept` no admite JSON, la respuesta es 406 sin cuerpo (D-089).

### Paginación
Las listas paginadas aceptan dos parámetros:
- `page`: número de página, **empezando en 0**;
- `size`: elementos por página, entre 1 y 100.

Su tamaño por defecto depende del recurso: 16 en ingredientes y 9 en recetas. Responden con este formato:
```json
{ "contenido": [], "pagina": 0, "tamano": 16, "totalElementos": 77, "totalPaginas": 5 }
```
Una página fuera de rango devuelve `contenido` vacío con los totales correctos. Un `page` negativo o un `size` fuera de rango responde 400 `VALIDACION` (D-026).

### Búsquedas por texto
Los filtros de texto buscan «contiene» sin distinguir mayúsculas ni acentos: «limon» encuentra «Limón». Los caracteres `%` y `_` se buscan literalmente (D-027).

Tienen como mucho 100 caracteres (`q`, `nombre`, `titulo`), salvo el autor de las recetas (`usuario`), 30, como un nombre de usuario. Con más, 400 `VALIDACION` con el error en ese parámetro (D-090).

## Autenticación (`/api/auth`, pública)

### `POST /api/auth/registro`
```json
{ "id": "lucia", "nombre": "Lucía", "apellido1": "Fernández", "apellido2": "Ruiz", "email": "lucia@ejemplo.es", "password": "demo1234" }
```

| Campo | Reglas |
|---|---|
| `id` | De 3 a 30 caracteres de `[a-zA-Z0-9._-]` |
| `nombre`, `apellido1` | Obligatorios |
| `apellido2` | Opcional |
| `email` | Válido y único |
| `password` | De 8 a 72 caracteres, y como mucho 72 bytes en UTF-8: la ñ y las letras con tilde ocupan 2 y un emoji, 4 (D-090). Igual en `POST /api/auth/password/restablecer` y en la `nueva` de `PUT /api/usuarios/me/password` |

La unicidad del `id` y del `email` no distingue mayúsculas ni acentos.

- **201:** el perfil creado (rol `USER`, `emailVerificado: false`); se envía el correo de bienvenida, con el enlace para confirmar el email (D-088).
- **409:** `errores.id` y/o `errores.email`.
- **400:** errores de validación.

### `POST /api/auth/login`
```json
{ "usuario": "lucia", "password": "demo1234" }
```
`usuario` admite el nombre de usuario o el email.

- **200:**
  ```json
  { "caduca": "2026-11-09T02:00:00Z", "usuario": { "id": "lucia", "nombre": "Lucía", "roles": ["USER"], "emailVerificado": true } }
  ```
  El JWT va en la cookie, no en el cuerpo.
- **401:** `CREDENCIALES_INVALIDAS`, con el mismo mensaje tanto si el usuario no existe como si la contraseña es incorrecta o pasa de 72 bytes, que no puede ser la de nadie (D-090).
- Si la contraseña es correcta y su hash es de un coste de bcrypt anterior, se vuelve a cifrar con el actual. No cierra las demás sesiones (D-090).
- **403:** `CUENTA_BLOQUEADA`, si la administración ha bloqueado la cuenta. Solo se dice cuando la contraseña es correcta (D-063).
- **429:** más de 10 intentos por minuto desde la misma IP, o 20 contraseñas fallidas en 15 minutos con ese usuario o email, desde cualquier IP. En este caso no se comprueba la contraseña, y el mensaje es el mismo exista o no la cuenta (D-090).

### `POST /api/auth/logout`
**204**. Borra la cookie de sesión de este navegador.

### `POST /api/auth/password/olvido`
```json
{ "email": "lucia@ejemplo.es" }
```
- **200**, siempre con el mismo mensaje, exista o no el email (D-021). Si existe, se envía un enlace `${APP_BASE_URL}/restablecer-password?token=…`, válido durante 1 hora y de un solo uso.
- **429:** más de 5 peticiones cada 15 minutos desde la misma IP.

### `POST /api/auth/password/restablecer`
```json
{ "token": "<el del enlace>", "password": "nueva-clave" }
```
- **200:** `{ "mensaje": "Contraseña cambiada. Ya puedes iniciar sesión." }`. El enlace llegó al email de la cuenta, así que ese email queda confirmado (D-088).
- **400:** `TOKEN_INVALIDO`.

### Confirmación del email (D-088)
El alta envía, en el correo de bienvenida, un enlace `${APP_BASE_URL}/confirmar-email?token=…`, válido 24 horas y de un solo uso. Cambiar el email desde el perfil envía otro al email nuevo, que no sustituye al anterior hasta que se confirma. Solo hay un enlace válido a la vez: pedir otro anula el anterior. Sin confirmar se puede usar todo salvo crear ingredientes (D-089); la interfaz lo recuerda (`usuario.emailVerificado` del login y `emailVerificado` del perfil).

Las cuentas creadas con Google tienen el email verificado desde el principio. Vincular con Google una cuenta con el email confirmado no pide la contraseña.

#### `POST /api/auth/email/confirmar`
```json
{ "token": "<el del enlace>" }
```
Sin sesión: el enlace puede abrirse en otro dispositivo.
- **200:** `{ "usuario": "lucia", "email": "lucia@nuevo.es" }`. Si era un cambio, la cuenta pasa a usar el email nuevo, se avisa por correo al anterior y el enlace de recuperación de la contraseña que se hubiera pedido deja de valer. Además, las sesiones abiertas de la cuenta dejan de valer (401); si la petición llega con la sesión de esa misma cuenta, la respuesta trae una cookie de sesión nueva y ese navegador sigue dentro (D-089).
- **400:** `TOKEN_INVALIDO`, si el enlace no vale, ya se usó o ha caducado.
- **409:** `EMAIL_EN_USO`, si mientras tanto otra cuenta ha empezado a usar el email nuevo.

#### `POST /api/usuarios/me/email/confirmacion`
Vuelve a enviar el enlace: al email nuevo si hay un cambio pendiente y, si no, al de la cuenta.
- **200:** `{ "email": "lucia@toca.local" }`, adónde se ha enviado.
- **409:** `EMAIL_YA_CONFIRMADO`, si está confirmado y no hay ningún cambio pendiente.
- **429:** más de 5 peticiones cada 15 minutos desde la misma IP.

#### `DELETE /api/usuarios/me/email/pendiente`
Descarta el email nuevo que esperaba su confirmación; su enlace deja de valer.
- **200:** el perfil actualizado (`emailPendiente: null`).

### Acceso con Google (D-085)
Solo funciona con `GOOGLE_CLIENT_ID` y `GOOGLE_CLIENT_SECRET`. Sin ellas, las rutas `/oauth2/**` y `/login/oauth2/**` no existen.

El navegador va a **`GET /oauth2/authorization/google`** (fuera de `/api`), que lleva a Google con el flujo de código de autorización y PKCE. Google vuelve a `/login/oauth2/code/google`, y el servidor valida su token de identidad y redirige:

| A | Cuándo |
|---|---|
| `/login/google` | La cuenta de Google ya estaba vinculada. Ya hay cookie de sesión |
| `/login/google` (también) | Su email verificado es el de una cuenta de Toca sin vincular **con el email confirmado**: se vincula sin pedir nada y se avisa por correo (D-088) |
| `/login/google/vincular` | Su email verificado es el de una cuenta de Toca sin vincular y **sin el email confirmado**: para vincularla hay que escribir su contraseña (D-086); después, el email queda confirmado. La vinculación pendiente va en la cookie `toca_vincular_google` (`HttpOnly`, `SameSite=Strict`, `Path=/api/auth/google`), válida 15 minutos |
| `/registro/google` | Nadie tiene ese email: hay un registro pendiente en la cookie `toca_registro_google` (`HttpOnly`, `SameSite=Strict`, `Path=/api/auth/google`), válido 15 minutos |
| `/login?google=bloqueada` | La cuenta está bloqueada (D-063) |
| `/login?google=email-no-verificado` | Google no da el email o no lo ha verificado |
| `/login?google=otra-cuenta` | La cuenta con ese email ya está vinculada con otra cuenta de Google |
| `/login?google=cancelado` · `/login?google=error` | La persona canceló en Google, o algo falló en el intercambio |

#### `GET /api/auth/google/vincular`
- **200:** `{ "email": "ana@gmail.com" }`, el email de Google, que es también el de la cuenta de Toca.
- **404:** `VINCULACION_GOOGLE_CADUCADA`, si no hay vinculación pendiente o ha caducado.

#### `POST /api/auth/google/vincular`
```json
{ "password": "su-clave-de-toca" }
```
Vincula la cuenta de Google con la de Toca de ese email si la contraseña es la de esta (D-086). Toca no comprueba los emails de sus cuentas: sin la contraseña, quien hubiera creado una cuenta con el email de otra persona se quedaría con su acceso con Google.
- **200:** como el login. Pone la cookie de sesión, borra la de la vinculación y avisa por correo al dueño de la cuenta.
- **400:** `PASSWORD_INCORRECTA` (`errores.password`), o `VALIDACION` si falta.
- **403:** `CUENTA_BLOQUEADA`, solo con la contraseña correcta, como en el login.
- **404:** `VINCULACION_GOOGLE_CADUCADA`.
- **409:** `YA_VINCULADA`, si la cuenta ya está vinculada con otra cuenta de Google.
- **429:** cuenta en el límite de las peticiones que comprueban la contraseña.

#### `GET /api/auth/proveedores`
**200:** `{ "google": true }` si el acceso con Google está configurado.

#### `GET /api/auth/sesion`
- **200:** lo mismo que el login (`caduca` y `usuario`), con los datos de la cookie de sesión. Lo usa `/login/google`.
- **401:** `SIN_SESION`.

#### `GET /api/auth/google/registro`
- **200:** lo que da Google, para el formulario: `{ "email": "nuria@gmail.com", "nombre": "Nuria", "apellido1": "Vidal", "apellido2": "Soto" }`. El primer apellido es la primera palabra de los apellidos de Google; el segundo, el resto o `null`.
- **404:** `REGISTRO_GOOGLE_CADUCADO`, si no hay registro pendiente o ha caducado.

#### `POST /api/auth/google/registro`
```json
{ "id": "nuria", "nombre": "Nuria", "apellido1": "Vidal", "apellido2": null }
```
Mismas reglas que `POST /api/auth/registro`, sin email ni contraseña. El email es el de Google y la contraseña, una aleatoria que nadie sabe: para tener una, se pide el enlace de `POST /api/auth/password/olvido`.

- **201:** como el login. Pone la cookie de sesión, borra la del registro y envía el correo de bienvenida.
- **404:** `REGISTRO_GOOGLE_CADUCADO`, también si esa cuenta de Google ya tiene usuario: un registro completado no sirve para entrar otra vez (D-086).
- **409:** `errores.id` (nombre de usuario en uso) o `errores.email` (el email ya lo usa otra cuenta).
- **400:** errores de validación.

## Perfil (`/api/usuarios/me`, con sesión)

### `GET /api/usuarios/me`
- **200:**
  ```json
  { "id": "lucia", "nombre": "Lucía", "apellido1": "Fernández", "apellido2": "Ruiz", "email": "lucia@toca.local", "roles": ["USER"], "google": false, "emailVerificado": true, "emailPendiente": null }
  ```
  `google` dice si la cuenta está vinculada con una cuenta de Google (D-085); `emailVerificado`, si ha confirmado el email, y `emailPendiente`, el email nuevo que espera su confirmación, o `null` (D-088).

### `PUT /api/usuarios/me`
```json
{ "nombre": "Lucía", "apellido1": "Fernández", "apellido2": null, "email": "lucia@nuevo.es", "password": "demo1234" }
```
`password` es la contraseña actual y solo hace falta si cambia el email (sin contar mayúsculas); si no, `null` (D-086).
- **200:** el perfil actualizado. Si cambia el email, el nuevo **no se usa todavía**: queda en `emailPendiente` y se le envía el enlace para confirmarlo; `email` sigue siendo el anterior hasta que se confirme (D-088).
- **400:** `PASSWORD_INCORRECTA` (`errores.password`) si cambia el email sin la contraseña o con otra.
- **409:** `errores.email` si otra cuenta ya usa ese email.

### `POST /api/usuarios/me/desvincular-google`
```json
{ "password": "demo1234" }
```
Deja de poder entrar con Google (D-086). Pide la contraseña: después será la única forma de entrar.
- **200:** el perfil actualizado (`google: false`). Si estaba vinculada, se avisa por correo.
- **400:** `PASSWORD_INCORRECTA` (`errores.password`), o `VALIDACION` si falta.

### `PUT /api/usuarios/me/password`
```json
{ "actual": "demo1234", "nueva": "otra-clave" }
```
- **204:** contraseña cambiada; también anula cualquier enlace de recuperación pendiente y **las demás sesiones abiertas**. La respuesta trae una cookie de sesión nueva para seguir en este dispositivo.
- **400:** `PASSWORD_INCORRECTA` (`errores.actual`).

### `GET /api/usuarios/me/datos`
Todos los datos del usuario, como fichero descargable (`Content-Disposition: attachment; filename="alimentarse-<id>.json"`, `Cache-Control: no-store`). No incluye la contraseña ni los tokens (D-064).
```json
{ "exportado": "2026-11-07T18:00:00Z",
  "perfil": { "id": "lucia", "nombre": "Lucía", "apellido1": "Fernández", "apellido2": "Ruiz",
              "email": "lucia@toca.local", "roles": ["USER"] },
  "recetas": [ { "id": 1, "titulo": "Tortilla…", "tipo": "Plato", "instrucciones": "<p>…</p>", "tiempo": 45,
                 "comensales": 4, "dificultad": "Media", "publicada": true, "oculta": false, "motivoOculta": null,
                 "fechaCreacion": "…", "imagen": "/api/imagenes/demo-tortilla.jpg",
                 "ingredientes": [ { "nombre": "Huevos", "cantidad": "6 unidades", "grupo": "Huevos" } ] } ],
  "consumos": [ { "fecha": "…", "receta": 10, "titulo": "Batido de plátano y fresa" } ],
  "meGusta": [ { "fecha": "…", "receta": 6, "titulo": "Pollo con tallarines" } ],
  "comidasPlanificadas": [ { "fecha": "…", "receta": 3, "titulo": "Lentejas estofadas" } ],
  "valoraciones": [ { "fecha": "…", "receta": 6, "titulo": "Pollo con tallarines", "nota": 4,
                      "comentario": null, "oculta": false } ],
  "avisos": [ { "fecha": "…", "servicio": "fcm.googleapis.com" } ] }
```
`avisos` son los navegadores con los avisos push activados, solo con el dominio de su servicio: la dirección completa permite enviarles avisos (D-078).

### `POST /api/usuarios/me/baja`
```json
{ "password": "demo1234" }
```
Elimina la cuenta (D-064). Es `POST` porque lleva la contraseña en el cuerpo.
- **204:** cuenta eliminada. Se borran:
  - el usuario y sus roles;
  - sus consumos y sus «me gusta»;
  - sus recetas, con sus imágenes y los «me gusta» que tenían.
  - Las recetas que **otros usuarios** habían consumido se conservan **eliminadas y sin autor** (`autor: null`), solo con el título y los ingredientes, para que esos consumos sigan contando (D-061).
  - Se envía un correo de confirmación, y la sesión abierta deja de valer (401).
- **400:** `PASSWORD_INCORRECTA` (`errores.password`), o `VALIDACION` si falta la contraseña.
- **409 `ULTIMO_ADMINISTRADOR`:** el único administrador no puede darse de baja; antes tiene que nombrar a otro.

## Catálogos (con sesión)

Los catálogos son de solo lectura. Los grupos y sus raciones se cambian con una migración (D-029).

### `GET /api/grupos`
Grupos de alimentos ordenados por nombre. `racionesSemana: null` significa sin límite (∞). Los administradores los editan con `/api/admin/grupos` (D-065).
```json
[ { "id": 8, "nombre": "Arroz", "racionesSemana": 4 }, { "id": 5, "nombre": "Fruta", "racionesSemana": null } ]
```

### `GET /api/tipos-receta`
```json
[ { "id": 4, "nombre": "Bebida" }, { "id": 5, "nombre": "Desayuno" } ]
```

## Ingredientes (con sesión)

Un ingrediente se devuelve con su grupo:
```json
{ "id": 36, "nombre": "Limón", "grupo": { "id": 5, "nombre": "Fruta", "racionesSemana": null },
  "alimento": "Zumo de limón, fresco" }
```

`alimento` es el nombre del alimento de BEDCA con el que se calcula su información nutricional (D-079, D-080), o `null` si no tiene.

### `GET /api/ingredientes?nombre=&grupo=&sinAlimento=false&page=0&size=16`
Página de ingredientes ordenados por nombre. Los filtros son opcionales y se combinan:
- `nombre`: contiene el texto;
- `grupo`: id del grupo;
- `sinAlimento`: con `true`, solo los que no tienen alimento de BEDCA (D-080).

### `GET /api/ingredientes/sugerencias?q=`
Para el autocompletado. Devuelve hasta 10 ingredientes: primero los que empiezan por `q` y después los que lo contienen (D-028). Si `q` está vacío, devuelve `[]`.

### `POST /api/ingredientes` (cualquier usuario con el email confirmado)
```json
{ "nombre": "Quinoa", "grupo": 6 }
```
El catálogo es de todos: hace falta el email confirmado y cada usuario crea como mucho 30 ingredientes al día. Los administradores no tienen ninguna de las dos condiciones (D-089).
- **201:** el ingrediente creado, con la cabecera `Location: /api/ingredientes/{id}`.
- **409:** `errores.nombre` si ya existe ese nombre (sin distinguir mayúsculas ni acentos).
- **400:** `errores.grupo` si el grupo no existe, o errores de validación.
- **403 `EMAIL_SIN_CONFIRMAR`:** el usuario no ha confirmado su email (D-088).
- **429 `LIMITE_INGREDIENTES`:** ya ha creado 30 en el último día; la cabecera `Retry-After` dice cuántos segundos faltan.

### `PUT /api/ingredientes/{id}` (solo ADMIN)
El cuerpo es igual que en el alta.
- **200:** el ingrediente actualizado.
- **403:** `ACCESO_DENEGADO` si quien edita no es administrador.
- **404:** el ingrediente no existe.
- **409:** el nombre ya lo usa otro ingrediente.

## Recetas (con sesión)

### Tarjeta de receta (`RecetaResumenDto`)
Es lo que devuelven los listados.
```json
{ "id": 1, "titulo": "Tortilla de patata rellena de jamón y queso", "autor": "lucia",
  "tipo": { "id": 1, "nombre": "Plato" }, "imagen": "/api/imagenes/demo-tortilla.jpg",
  "tiempo": 45, "comensales": 4, "dificultad": "Media", "likes": 3, "meGusta": false,
  "nota": 4.5, "valoraciones": 2,
  "publicada": true, "fechaCreacion": "2026-10-06T21:00:00Z", "eliminada": false }
```
- `imagen` es una URL relativa, o `null` si la receta no tiene imagen.
- `meGusta` indica si el usuario actual le ha dado «me gusta».
- `nota` es la media de sus valoraciones visibles, con un decimal, o `null` si no tiene; `valoraciones`, cuántas son (D-077).
- `oculta` indica que la ha ocultado un administrador (D-063). Solo la ven su autor y los administradores, así que fuera de «Mis recetas» y de la moderación casi siempre es `false`.
- `eliminada` solo es `true` en los consumos (histórico y «Mi alimentación»): su autor la eliminó cuando alguien ya la había consumido. Llega sin imagen y no se puede abrir (D-061).

### `GET /api/recetas`
Recetas **publicadas**, de la más reciente a la más antigua, con `size=9` por defecto (D-031). Filtros de la sección 6.5, todos opcionales y combinables:

| Parámetro | Condición |
|---|---|
| `titulo` | El título contiene el texto, sin distinguir mayúsculas ni acentos |
| `tipo` | Id del tipo de receta |
| `usuario` | El autor contiene el texto |
| `dificultad` | `Fácil`, `Media` o `Difícil` |
| `comensales` | La receta es para **al menos** N comensales (≥ 1) |
| `tiempoMax` | Tarda **como mucho** N minutos (≥ 1) |
| `ingredientes` | Ids separados por comas (`1,2`) o el parámetro repetido; la receta debe contenerlos **todos**. Como mucho 50, los que puede tener una receta (D-090) |

Un valor que no se puede interpretar (por ejemplo, `tipo=abc`) responde 400 `VALIDACION` con «Valor no válido» en ese campo.

### `GET /api/recetas/mias`
Las recetas propias, publicadas o no. Admite los mismos filtros y la misma paginación.

### `GET /api/recetas/me-gustan`
«Recetas que me gustan»: las **publicadas** a las que el usuario ha dado «me gusta», de la que le gustó más recientemente a la más antigua (D-062). Admite los mismos filtros y la misma paginación. Si el autor deja de publicar una, deja de salir aquí; si la vuelve a publicar, reaparece con su «me gusta».

### `GET /api/recetas/mejor-valoradas?limit=3` · `GET /api/recetas/ultimas?limit=3`
Para la portada; `limit` va de 1 a 20.
- Mejor valoradas: por la nota media ponderada, como si cada receta tuviera además dos valoraciones de 3, para que una sola nota de 5 no adelante a muchas de 4 (D-077). Una receta sin valoraciones queda en 3. En empate, la de más «me gusta» y después la más reciente.
- Últimas: por fecha de creación, de la más reciente a la más antigua.

### `GET /api/recetas/sugerencias?q=`
Buscador de la portada. Devuelve hasta 8 `{ "id", "titulo" }` de recetas publicadas: primero las que empiezan por `q` y después las que lo contienen.

### `GET /api/recetas/{id}`
Detalle de la receta: los campos de la tarjeta, más estos:
```json
{ "instrucciones": "<p>…</p><ol><li>…</li></ol>",
  "ingredientes": [ { "id": 26, "nombre": "Patata", "cantidad": "600 gramos",
                      "grupo": { "id": 4, "nombre": "Hortalizas", "racionesSemana": null } } ],
  "grupos": [ { "id": 12, "nombre": "Carne grasa", "racionesSemana": 2 } ],
  "maxSemanal": 2, "esAutor": false, "motivoOculta": null,
  "miValoracion": { "id": 4, "usuario": "marcos", "nota": 5, "comentario": "Muy jugosa.",
                    "fecha": "2026-10-24T18:00:00Z", "oculta": false },
  "fotos": [ { "nombre": "demo-tortilla.jpg", "url": "/api/imagenes/demo-tortilla.jpg", "descripcion": null },
             { "nombre": "demo-tortilla-corte.jpg", "url": "/api/imagenes/demo-tortilla-corte.jpg",
               "descripcion": "La tortilla cortada, con el relleno de jamón cocido y queso" } ] }
```

| Campo | Qué es |
|---|---|
| `instrucciones` | HTML ya saneado; se puede insertar tal cual |
| `grupos` | Los grupos de sus ingredientes, sin repetir y ordenados por nombre |
| `maxSemanal` | Veces a la semana que se puede comer como máximo: el mínimo de las raciones de sus grupos (6.1). `null` = sin límite |
| `motivoOculta` | Si la administración la ha ocultado (`oculta: true`), el motivo; si no, `null` |
| `miValoracion` | La valoración del usuario actual, aunque la administración la haya ocultado, o `null` |
| `fotos` | La galería, en orden (D-083): la primera es la principal, la de `imagen`. `nombre` sirve para conservar la foto al editar la receta; `descripcion` es su texto alternativo, o `null` |
| `video` | Enlace a un vídeo de la receta en YouTube o Vimeo, o `null` (D-083) |

Una receta **no publicada** responde 404 a quien no es su autor (D-032). Una receta **oculta** por la administración responde 404 a todos menos a su autor y a los administradores (D-063).

### `POST /api/recetas` (multipart)
Partes (D-036, D-083):
- `receta` (**obligatoria**, `Content-Type: application/json`);
- `fotos` (**opcionales**, una parte por fichero nuevo: JPEG, PNG o WebP de 5 MB como máximo). Cada una se guarda recodificada a JPEG, con 1600 píxeles como máximo en su lado mayor y sin metadatos (D-069).

Contenido de la parte `receta`:
```json
{ "titulo": "Revuelto de setas", "instrucciones": "<p>Paso 1…</p>", "tipo": 1, "tiempo": 15,
  "comensales": 2, "dificultad": "Fácil",
  "ingredientes": [ { "ingrediente": 56, "cantidad": "3 unidades" } ],
  "fotos": [ { "existente": null, "nueva": 1, "descripcion": "El revuelto en el plato" },
             { "existente": null, "nueva": 0, "descripcion": null } ],
  "video": "https://www.youtube.com/watch?v=…" }
```

`video` es opcional: un enlace `https` a un vídeo de YouTube o Vimeo, que se muestra como un enlace, no incrustado (D-083). Vacío o `null`, la receta no tiene vídeo.

`fotos` es la galería completa, en orden, como mucho 6; la primera es la principal. Cada foto es **o** una que ya tiene la receta (`existente`: su `nombre`, solo al editar) **o** un fichero nuevo (`nueva`: su posición entre las partes `fotos`, desde 0). En el ejemplo, la segunda parte `fotos` es la principal. Sin `fotos` (`null`), la receta no tiene fotos al crearla, y al editarla conserva las que tenía.

Así se envía desde el navegador:
```ts
const datos = new FormData();
datos.append('receta', new Blob([JSON.stringify(receta)], { type: 'application/json' }));
for (const fichero of ficheros) datos.append('fotos', fichero);
http.post('/api/recetas', datos); // sin fijar Content-Type: lo pone el navegador con el boundary
```

Respuestas:
- **201:** el detalle de la receta, **sin publicar** (6.6), con la cabecera `Location`.
- **400 `VALIDACION`**, con errores por campo (D-037):

  | Campo | Regla |
  |---|---|
  | `titulo` | Obligatorio, ≤ 100 |
  | `instrucciones` | Obligatorio, ≤ 20 000; no puede quedar vacío tras sanear el HTML |
  | `tipo` | Debe existir |
  | `tiempo` | De 1 a 1440 |
  | `comensales` | De 1 a 100 |
  | `dificultad` | `Fácil`, `Media` o `Difícil` |
  | `ingredientes` | Entre 1 y 50 (D-089); sin repetir y existentes |
  | `ingredientes[n].cantidad` | Obligatoria |
  | `fotos` | Como mucho 6; ningún fichero que no use ninguna foto |
  | `fotos[n]` | Una foto existente o un fichero nuevo, no las dos. Si es nuevo, su tipo real (por su contenido) debe ser JPEG, PNG o WebP, y debe poder leerse (25 megapíxeles como máximo, D-089) |
  | `fotos[n].existente` | Una foto de esa receta, sin repetir |
  | `fotos[n].nueva` | La posición de una parte `fotos` con contenido, sin repetir |
  | `fotos[n].descripcion` | ≤ 150 |
  | `video` | ≤ 500; `https` a `youtube.com`, `www.youtube.com`, `m.youtube.com`, `youtu.be`, `vimeo.com`, `www.vimeo.com` o `player.vimeo.com` |

- **413 `IMAGEN_DEMASIADO_GRANDE`** (`errores.fotos`): una foto supera los 5 MB o la petición, los 32 MB.
- **409 `LIMITE_RECETAS`** (solo el alta): el autor ya tiene 500 recetas sin eliminar (D-089).

### `PUT /api/recetas/{id}` (multipart, solo el autor)
Se envía igual que el alta.
- La galería queda como dice `fotos`; las fotos que tenía y ya no están se borran. Sin `fotos`, se conserva la que tenga (D-083).
- Los ingredientes se sustituyen por los enviados.
- No cambia ni el estado de publicación ni la fecha de creación.
- **403** si la receta es de otro usuario (o **404** si, además, no está publicada).

### `PATCH /api/recetas/{id}/publicada` (solo el autor)
```json
{ "publicada": true }
```
Devuelve el detalle actualizado. **409 `RECETA_OCULTA`** si se intenta publicar una receta oculta por la administración; dejar de publicarla sí se puede.

### `DELETE /api/recetas/{id}` (solo el autor)
- **204:** eliminada.
  - Si **nadie la ha consumido**, se borra del todo, con sus ingredientes, sus «me gusta» y sus fotos.
  - Si **alguien la ha consumido**, se marca como eliminada: deja de estar publicada y pierde las fotos y los «me gusta». Conserva el título y los ingredientes para que esos consumos sigan contando en las raciones de los últimos 7 días y en el histórico (D-061).
- Una receta eliminada no existe para nadie, ni siquiera para su autor: **404** en el detalle, la edición, la publicación, los «me gusta», los consumos nuevos y otra eliminación. Tampoco sale en ningún listado.
- **403** si es de otro usuario (o **404** si, además, no está publicada).

### `POST /api/recetas/{id}/like` · `DELETE /api/recetas/{id}/like`
Dar o quitar «me gusta». Son idempotentes y devuelven `{ "likes": 3, "meGusta": true }`. Repetir un «me gusta» no cambia su fecha. Dar «me gusta» a una receta no publicada, o a una oculta por la administración, responde 400 `RECETA_NO_PUBLICADA` (D-033).

### Valoraciones (D-077)
Cada usuario puede poner a una receta una nota de 1 a 5 y un comentario opcional, en texto plano, y cambiarlos o borrarlos. Conviven con los «me gusta», que son los favoritos de cada uno. Nadie valora su propia receta.

#### `GET /api/recetas/{id}/valoraciones?page=0&size=5`
Página de valoraciones de la receta, de la más reciente a la más antigua:
```json
{ "contenido": [ { "id": 4, "usuario": "marcos", "nota": 5, "comentario": "Muy jugosa.",
                   "fecha": "2026-10-24T18:00:00Z", "oculta": false } ],
  "pagina": 0, "tamano": 5, "totalElementos": 2, "totalPaginas": 1 }
```
- `fecha` es la de la última vez que se guardó.
- Las ocultas por la administración solo se listan a los administradores, con `oculta: true`, para poder volver a mostrarlas.
- **404** si el usuario no puede ver la receta.

#### `PUT /api/recetas/{id}/valoracion`
```json
{ "nota": 4, "comentario": "Muy jugosa." }
```
Crea o sustituye la valoración propia. El comentario es opcional (`null` o en blanco) y se guarda sin espacios a los lados ni caracteres de control, salvo los saltos de línea. Si la administración la había ocultado, sigue oculta.
- **200:** cómo queda la receta: `{ "nota": 4.5, "valoraciones": 2, "mia": { … } }`.
- **400 `VALIDACION`:** `errores.nota` si falta o no va de 1 a 5; `errores.comentario` si pasa de 1000 caracteres.
- **403 `PROPIA_RECETA`:** es una receta propia.
- **404:** el usuario no puede ver la receta (no existe, no está publicada, está oculta o eliminada).

#### `DELETE /api/recetas/{id}/valoracion`
Borra la propia. Es idempotente y devuelve lo mismo que el `PUT`, con `mia: null`. **404** si el usuario no puede ver la receta.

### `GET /api/recetas/recomendadas` — «¿Qué toca hoy?» (6.3)
Página de recetas **publicadas** que **no tienen ningún ingrediente de un grupo agotado** por el usuario en los últimos 7 días. Un grupo está agotado si tiene límite semanal y ya se han consumido al menos esas raciones. Las propias recetas publicadas también pueden salir.

- **Orden (D-067):** favorece la variedad, en tres tramos:
  1. las recetas sin ningún grupo con límite que ya se haya comido esta semana;
  2. las que llevan alguno, con el indicador en verde;
  3. las que llevan alguno con el indicador en ámbar.

  Dentro de cada tramo, por número de likes, de más a menos, y en empate la más reciente.
- **Parámetros:** los mismos filtros y la misma paginación que `GET /api/recetas`, con 9 por página por defecto.
- **Respuesta:** cada elemento es la tarjeta de siempre más `grupos`, con cómo van esta semana los grupos de la receta, en el formato de los indicadores de `GET /api/alimentacion/resumen`. Primero los grupos con límite, de más a menos llenos. No incluye los grupos básicos sin límite, como Especias (D-054).

```json
{ "id": 7, "titulo": "Macarrones con tomate", "likes": 2, …,
  "grupos": [
    { "grupo": { "id": 7, "nombre": "Pasta", "racionesSemana": 4 }, "consumidas": 1, "limite": 4, "porcentaje": 25, "color": "VERDE" },
    { "grupo": { "id": 4, "nombre": "Hortalizas", "racionesSemana": null }, "consumidas": 0, "limite": null, "porcentaje": 100, "color": "VERDE" }
  ] }
```

Si no queda ninguna, `contenido` llega vacío (D-041).

### `GET /api/recetas/{id}/impacto`
Cómo quedaría cada grupo **con límite** de la receta si el usuario la comiera ahora: una ración más sobre las de los últimos 7 días, en el orden de los indicadores (D-067).

```json
[ { "antes":   { "grupo": { "id": 11, "nombre": "Carne", "racionesSemana": 3 }, "consumidas": 2, "limite": 3, "porcentaje": 67, "color": "VERDE" },
    "despues": { "grupo": { "id": 11, "nombre": "Carne", "racionesSemana": 3 }, "consumidas": 3, "limite": 3, "porcentaje": 100, "color": "ROJO" } } ]
```

- La lista está vacía si la receta solo lleva grupos sin límite.
- **404:** la receta no existe o el usuario no la puede ver, igual que al registrar un consumo.

### `GET /api/recetas/{id}/nutricion`
Información nutricional **estimada** de la receta con la composición de BEDCA de sus ingredientes (D-079): de la receta entera (`total`) y por ración (`porRacion`, el total entre los comensales).

```json
{ "comensales": 2,
  "total":     { "energiaKcal": 811, "energiaKj": 3394, "grasas": 11.1, "grasasSaturadas": 3.1, "hidratos": 143.0,
                 "fibra": 7.0, "proteinas": 32.5, "sal": 0.37 },
  "porRacion": { "energiaKcal": 406, "energiaKj": 1697, "grasas": 5.6, "grasasSaturadas": 1.6, "hidratos": 71.5,
                 "fibra": 3.5, "proteinas": 16.3, "sal": 0.18 },
  "ingredientes": [
    { "id": 7,  "nombre": "Harina",   "cantidad": "200 gramos", "gramos": 200.0, "alimento": "Harina de trigo", "estado": "CALCULADO" },
    { "id": 57, "nombre": "Huevos",   "cantidad": "2",          "gramos": 100.0, "alimento": "Huevo de gallina fresco", "estado": "CALCULADO" },
    { "id": 9,  "nombre": "Levadura", "cantidad": "1 sobre",    "gramos": null,  "alimento": null, "estado": "SIN_DATOS" },
    { "id": 3,  "nombre": "Sal",      "cantidad": "1 pizca",    "gramos": null,  "alimento": "Sal de mar", "estado": "SIN_PESO" } ] }
```

- Energía en kcal y kJ, sin decimales; grasas, saturadas, hidratos, fibra y proteínas en gramos, con un decimal; sal en gramos, con dos, calculada con el sodio (× 2,5).
- `ingredientes`: todos los de la receta, en el orden del detalle. `estado`:
  - `CALCULADO`: cuenta con los `gramos` indicados y la composición del `alimento` de BEDCA;
  - `SIN_DATOS`: el ingrediente no tiene composición;
  - `SIN_PESO`: su cantidad no se puede pasar a gramos («al gusto», «1 pizca», una pieza sin peso aproximado).
- `total` y `porRacion` son `null` si ningún ingrediente cuenta.
- **404:** la receta no existe o el usuario no la puede ver (D-032); los administradores ven también las ocultas.

## Planificador (con sesión, siempre del usuario actual)

Comidas futuras y cómo quedarían las raciones si se cumple el plan (D-068).

### `GET /api/plan?desde=…&hasta=…`
Instantes ISO-8601, ambos obligatorios. El intervalo debe ser positivo y de 31 días como máximo; si no, **400**.

```json
{ "comidas": [ { "id": 7, "fecha": "2026-11-09T13:00:00Z", "disponible": true,
                 "receta": { "id": 6, "titulo": "Pollo con tallarines", … },
                 "excesos": [ { "grupo": { "id": 11, "nombre": "Carne", "racionesSemana": 3 }, "consumidas": 4, "limite": 3, "porcentaje": 100, "color": "ROJO" } ] } ],
  "indicadores": [ { "grupo": { … }, "consumidas": 2, "limite": 3, "porcentaje": 67, "color": "VERDE" } ] }
```

- `comidas`: las del intervalo, por fecha.
- `disponible`: `false` si el usuario ya no puede ver la receta (despublicada, oculta o eliminada). Esa comida no cuenta.
- `excesos`: los grupos de la receta que superarían su límite en ese momento, contando lo comido y lo planificado en los 7 días anteriores. Llegar justo al límite no es un exceso.
- `indicadores`: como los de `GET /api/alimentacion/resumen`, pero calculados en `hasta`, con lo comido y lo planificado.
- Una comida cuyo momento ya ha pasado no cuenta, salvo que se haya registrado como consumo.

### `GET /api/plan/compra?desde=…&hasta=…`
Lista de la compra (D-076): los ingredientes de las comidas del intervalo que aún no han pasado. Mismos parámetros y mismo **400** que `GET /api/plan`.

```json
{ "comidas": [ { "id": 7, "fecha": "2026-11-14T13:00:00Z", "receta": 1, "titulo": "Tortilla de patata rellena de jamón y queso" } ],
  "secciones": [
    { "grupo": { "id": 10, "nombre": "Huevos", "racionesSemana": 4 }, "basico": false,
      "articulos": [ { "ingrediente": 5, "nombre": "Huevos", "total": "13 unidades",
                       "cantidades": [ { "receta": 9, "titulo": "Rollos de canela con chocolate blanco", "cantidad": "1 unidad", "veces": 1 },
                                       { "receta": 1, "titulo": "Tortilla de patata rellena de jamón y queso", "cantidad": "6 unidades", "veces": 2 } ] } ] },
    { "grupo": { "id": 1, "nombre": "Esenciales", "racionesSemana": null }, "basico": true,
      "articulos": [ { "ingrediente": 30, "nombre": "Sal", "total": null, "cantidades": [ … ] } ] } ],
  "noDisponibles": 0 }
```

- `comidas`: las que entran en la lista, por fecha: las del intervalo posteriores a ahora, con la receta disponible para el usuario.
- `secciones`: por grupo de alimentos, en orden alfabético, con los grupos básicos (`basico: true`: sal, aceite, especias…) al final. Dentro de cada una, los ingredientes por nombre.
- `cantidades`: lo que pide cada receta, tal como lo escribió su autor, y `veces`, cuántas comidas del intervalo son de esa receta.
- `total`:
  - la suma, si todas las cantidades empiezan por un número y llevan la misma unidad, en singular o en plural («1 unidad» y «6 unidades» × 2 son «13 unidades»);
  - la cantidad tal cual, si solo la pide una comida;
  - `null` si no se puede sumar («1 pizca» y «al gusto»).
- `noDisponibles`: comidas que no entran porque su receta ya no está disponible.

### `POST /api/plan`
```json
{ "receta": 6, "fecha": "2026-11-09T13:00:00Z" }
```
- **201:** la comida creada, con la cabecera `Location`.
- **400:** `errores.fecha` si el momento ya ha pasado (se toleran 60 s de desfase de reloj) o está a más de 90 días.
- **404:** la receta no existe o el usuario no la puede ver.
- **409 `HUECO_OCUPADO`:** el usuario ya tiene una comida en ese mismo momento.
- **409 `LIMITE_PLANIFICADAS`:** el usuario ya tiene 200 comidas planificadas que aún no han llegado (D-089).

### `DELETE /api/plan/{id}`
**204**. **404** si no existe o es de otro usuario.

### `POST /api/plan/{id}/consumir`
Registra la comida como consumo y la quita del plan. La fecha del consumo es la de la comida o, si aún no ha llegado, ahora.
- **201:** el consumo creado, con la cabecera `Location`.
- **404:** la comida no existe o es de otro usuario, o la receta ya no se puede ver. En este último caso sigue en el plan.
- **429 `LIMITE_CONSUMOS`:** como en `POST /api/consumos` (D-090); la comida sigue en el plan.

## Avisos push (con sesión)

El recordatorio de la comida y la cena planificadas llega como aviso push al navegador, una hora antes (D-078). Los avisos solo funcionan si el servidor tiene claves VAPID (`PUSH_CLAVE_PUBLICA` y `PUSH_CLAVE_PRIVADA`); si no, `GET /api/push/clave` devuelve `null` y lo demás responde **409 `AVISOS_DESACTIVADOS`**.

### `GET /api/push/clave`
`{ "clavePublica": "BP4z9KsN…" }`: la clave pública VAPID en Base64 URL, que el navegador necesita para suscribirse (`applicationServerKey`). `null` si no hay avisos.

### `PUT /api/push/suscripcion`
```json
{ "endpoint": "https://fcm.googleapis.com/fcm/send/…", "p256dh": "BCVxsr7N…", "auth": "BTBZMqHH…",
  "zona": "Europe/Madrid" }
```
Guarda la suscripción de este navegador (`PushSubscription.toJSON()`, más su zona horaria IANA, opcional: por defecto la de Madrid). Si ese navegador ya estaba suscrito, la actualiza y pasa a ser del usuario actual.
- **204.**
- **400 `VALIDACION`:**
  - `errores.endpoint` si no es una URL `https` del puerto estándar de un servicio push conocido (`fcm.googleapis.com`, `updates.push.services.mozilla.com`, `*.notify.windows.com`, `*.push.apple.com`);
  - `errores.p256dh` y `errores.auth` si las claves del navegador no tienen 65 y 16 bytes, o si la pública no es un punto de la curva P-256 (D-089);
  - `errores.zona` si la zona no existe.
- **409 `LIMITE_NAVEGADORES`:** el usuario ya tiene los avisos activados en otros 10 navegadores; volver a suscribir uno de ellos no cuenta (D-089).

### `DELETE /api/push/suscripcion?endpoint=…`
Borra la suscripción de ese navegador, si es del usuario actual. **204**, también si no existía.

### `POST /api/push/prueba`
Envía un aviso de prueba a todos los navegadores del usuario. Devuelve `{ "enviados": 1 }`: a cuántos lo ha aceptado su servicio push. **409 `SIN_SUSCRIPCIONES`** si no tiene ninguno. Las suscripciones que el servicio push ya no conoce (404 o 410) se borran.

### El recordatorio
Cada cinco minutos, el servidor busca las comidas planificadas que empiezan dentro de la próxima hora y no tienen recordatorio, de usuarios no bloqueados con algún navegador suscrito y con la receta aún disponible para ellos. Envía uno a cada navegador:
```json
{ "notification": { "title": "Comida: Lentejas estofadas", "body": "Hoy a las 14:00, según tu planificador.",
                    "icon": "/icons/icono-192.png", "lang": "es", "tag": "comida-7",
                    "data": { "onActionClick": { "default": { "operation": "navigateLastFocusedOrOpen",
                                                              "url": "/recetas/3" } } } } }
```
Es el formato del service worker de Angular, que muestra el aviso y, al pulsarlo, abre la receta. Va cifrado para ese navegador (RFC 8291) y firmado con VAPID (RFC 8292). Cada comida se avisa una sola vez, aunque haya varios servidores.

## Consumos (con sesión, siempre del usuario actual)

Un consumo se devuelve con la tarjeta de su receta:
```json
{ "id": 15, "fecha": "2026-11-05T08:41:38Z", "receta": { "id": 10, "titulo": "Batido de plátano y fresa", … } }
```
Las fechas son instantes ISO‑8601 en UTC. El frontend convierte el `datetime-local` del navegador a UTC antes de enviarlo y lo muestra en hora local.

### `POST /api/consumos`
```json
{ "receta": 10, "fecha": "2026-11-05T08:30:00Z" }
```
- `fecha` es opcional: si falta o es `null`, se usa «ahora».
- **201:** el consumo creado, con la cabecera `Location`.
- **400:** `errores.fecha` si la fecha es futura (se toleran 60 s de desfase de reloj) o `errores.receta` si falta la receta.
- **404:** la receta no existe, está eliminada o el usuario no la puede ver (no publicada y ajena).
- **429 `LIMITE_CONSUMOS`:** ya ha registrado 50 en el último día, también contando las comidas del planificador marcadas como comidas (D-090). La cabecera `Retry-After` dice cuántos segundos faltan.

### `GET /api/consumos?page=0&size=9`
Histórico paginado del usuario, del más reciente al más antiguo.

### `PUT /api/consumos/{id}`
```json
{ "fecha": "2026-11-04T20:30:00Z" }
```
- **200:** el consumo actualizado.
- **400:** fecha futura.
- **404:** el consumo no existe o es de otro usuario (D-039).

### `DELETE /api/consumos/{id}`
- **204:** borrado.
- **404:** el consumo no existe o es de otro usuario.

Cualquier cambio en los consumos se refleja de inmediato en el resumen y en la recomendación.

## Mi alimentación (con sesión)

### `GET /api/alimentacion/resumen`
```json
{
  "ultimas": [ { "id": 15, "fecha": "…", "receta": { … } } ],
  "masConsumidas": [ { "receta": { … }, "veces": 3 } ],
  "ingredientesMasUsados": [ { "ingrediente": { "id": 57, "nombre": "Pollo", "grupo": { … } }, "veces": 5 } ],
  "grupos7Dias": [
    { "grupo": { "id": 11, "nombre": "Carne", "racionesSemana": 3 }, "consumidas": 3, "limite": 3, "porcentaje": 100, "color": "ROJO" },
    { "grupo": { "id": 7, "nombre": "Pasta", "racionesSemana": 4 }, "consumidas": 3, "limite": 4, "porcentaje": 75, "color": "AMARILLO" },
    { "grupo": { "id": 5, "nombre": "Fruta", "racionesSemana": null }, "consumidas": 3, "limite": null, "porcentaje": 100, "color": "VERDE" }
  ]
}
```

| Campo | Contenido |
|---|---|
| `ultimas` | Los 3 consumos más recientes |
| `masConsumidas` | Las 3 recetas más consumidas en **todo el historial**; en empate, la consumida más recientemente |
| `ingredientesMasUsados` | Los 4 ingredientes presentes en más consumos, también en todo el historial. No se cuentan los de los grupos básicos, Esenciales (sal, aceite, agua…) y Especias, que están en casi todas las recetas (D-054); por eso puede haber menos de 4 |
| `grupos7Dias` | Un indicador por cada grupo con raciones en los últimos 7 días (6.4) |

Cálculo de cada indicador de `grupos7Dias`:
- `porcentaje` = `consumidas/limite` (con un tope de 100). Sin límite, `limite` es `null` y el porcentaje, 100.
- `color`:
  - `VERDE` si no hay límite o la proporción es menor que 0,75;
  - `AMARILLO` si está entre 0,75 y 1 (sin llegar a 1);
  - `ROJO` si llega a 1 o la supera.
- Orden: primero los grupos con límite, de más a menos lleno, y después los ilimitados por nombre (D-040).

### `GET /api/alimentacion/nutricion`
Nutrientes estimados de lo que el usuario ha comido en los últimos 7 días, la misma ventana que `grupos7Dias` (D-082). Cada consumo cuenta como **una ración** de su receta, con la estimación de `GET /api/recetas/{id}/nutricion` (D-079).

```json
{ "consumos": 3, "estimados": 2,
  "total":       { "energiaKcal": 811, "energiaKj": 3394, "grasas": 11.1, "grasasSaturadas": 3.1, "hidratos": 143.0,
                   "fibra": 7.0, "proteinas": 32.5, "sal": 0.37 },
  "mediaDiaria": { "energiaKcal": 116, "energiaKj": 485, "grasas": 1.6, "grasasSaturadas": 0.4, "hidratos": 20.4,
                   "fibra": 1.0, "proteinas": 4.6, "sal": 0.05 },
  "referencia":  { "energia": 6, "grasas": 2, "grasasSaturadas": 2, "hidratos": 8, "proteinas": 9, "sal": 1 } }
```

| Campo | Contenido |
|---|---|
| `consumos` | Consumos registrados en los últimos 7 días |
| `estimados` | De ellos, los de recetas con estimación (algún ingrediente cuenta) |
| `total` | La suma de una ración de cada consumo estimado, redondeada como en las recetas |
| `mediaDiaria` | El total entre 7, aunque algún día no se registrara nada |
| `referencia` | `mediaDiaria` en porcentaje (entero) de la ingesta de referencia de un adulto medio del Reglamento (UE) n.º 1169/2011: 8.400 kJ, 70 g de grasas, 20 g de saturadas, 260 g de hidratos, 50 g de proteínas y 6 g de sal. Sin fibra, que no tiene |

`total`, `mediaDiaria` y `referencia` son `null` si ningún consumo tiene estimación (o no hay consumos).

## Administración (solo ADMIN)

Además del rol ADMIN en el token, se comprueba que el solicitante **siga siendo administrador en la base de datos**. Si ya no lo es, la respuesta es 403 `ACCESO_DENEGADO` (D-043).

### `GET /api/admin/usuarios?q=&page=0&size=10`
Página de usuarios ordenada por id. `q` es opcional y busca «contiene», sin distinguir mayúsculas ni acentos, en el id, el nombre, los apellidos o el email (D-044).
```json
{ "contenido": [ { "id": "lucia", "nombre": "Lucía", "apellido1": "Fernández", "apellido2": "Ruiz",
                   "email": "lucia@toca.local", "admin": false, "bloqueado": false } ],
  "pagina": 0, "tamano": 10, "totalElementos": 1, "totalPaginas": 1 }
```

### `PUT /api/admin/usuarios/{id}/admin`
```json
{ "admin": true }
```
Da (`true`) o quita (`false`) el rol de administrador. Es idempotente.
- **200:** el usuario actualizado. El cambio se aplica a la persona afectada **en su siguiente inicio de sesión**, porque los roles viajan en su token (D-019). Si se le quitó el rol, ya no puede usar estos endpoints aunque su token aún diga ADMIN.
- **400 `PROPIO_ROL`:** un administrador no puede quitarse el rol a sí mismo.
- **409 `ULTIMO_ADMINISTRADOR`:** debe quedar al menos un administrador.
- **409 `USUARIO_BLOQUEADO`:** no se puede hacer administrador a un usuario bloqueado.
- **404:** el usuario no existe.
- **400 `VALIDACION`:** falta `admin`.

### `PUT /api/admin/usuarios/{id}/bloqueado`
```json
{ "bloqueado": true }
```
Bloquea (`true`) o desbloquea (`false`) a un usuario. Es idempotente (D-063).
- **200:** el usuario actualizado. Un usuario bloqueado no puede iniciar sesión (403 `CUENTA_BLOQUEADA`), y su sesión abierta deja de valer **en la siguiente petición** (401 `CUENTA_BLOQUEADA`). Sus recetas y sus datos no cambian. Sus sesiones anteriores tampoco vuelven a valer al desbloquearlo: tendrá que iniciar sesión otra vez (D-089).
- **400 `PROPIO_BLOQUEO`:** un administrador no puede bloquearse a sí mismo.
- **409 `ADMIN_NO_BLOQUEABLE`:** a un administrador no se le puede bloquear; antes hay que quitarle el rol.
- **404:** el usuario no existe. **400 `VALIDACION`:** falta `bloqueado`.

### `GET /api/admin/recetas/ocultas?page=0&size=9`
Página de tarjetas de las recetas ocultas por la administración, de la más reciente a la más antigua.

### `PUT /api/admin/recetas/{id}/oculta`
```json
{ "oculta": true, "motivo": "Las cantidades no son realistas." }
```
Oculta una receta o la vuelve a mostrar (D-063).
- **200:** el detalle actualizado.
- Al ocultarla, `motivo` es obligatorio (hasta 500 caracteres) y lo verá el autor. Mientras esté oculta, solo la ven su autor y los administradores; no sale en ningún listado, ni en la portada ni en «¿Qué toca hoy?», no recibe «me gusta» y su autor no puede publicarla. Los consumos ya registrados no cambian.
- Al volver a mostrarla (`oculta: false`), se borra el motivo y vuelve a su estado de publicación.
- **400 `VALIDACION`:** falta `oculta` o el motivo (`errores.motivo`).
- **404:** la receta no existe, está eliminada o no está publicada (el administrador no puede verla).

### `PUT /api/admin/valoraciones/{id}/oculta`
```json
{ "oculta": true }
```
Oculta una valoración o la vuelve a mostrar (D-077). Oculta, solo la ven su autor (en `miValoracion`) y los administradores, y no cuenta en la nota media.
- **200:** la valoración actualizada.
- **400 `VALIDACION`:** falta `oculta`.
- **404:** la valoración no existe.

### `GET /api/admin/grupos`
Los grupos de alimentos, por nombre, con lo que necesita la administración (D-065):
```json
[ { "id": 11, "nombre": "Carne", "racionesSemana": 3, "basico": false, "ingredientes": 12 } ]
```
`basico` marca los grupos que no salen en «Ingredientes más usados» (D-054). `ingredientes` es cuántos ingredientes lo usan.

### `POST /api/admin/grupos` · `PUT /api/admin/grupos/{id}`
```json
{ "nombre": "Marisco", "racionesSemana": 2, "basico": false }
```
- `nombre`: obligatorio, hasta 100 caracteres y único (sin distinguir mayúsculas ni acentos).
- `racionesSemana`: de 1 a 99, o `null` para «sin límite».
- `basico`: obligatorio.
- **201** (alta, con `Location`) o **200** (edición): el grupo.
- **409** con `errores.nombre` si el nombre ya existe; **400** `VALIDACION`; **404** si el grupo no existe.
- **El cambio se aplica al momento** a los indicadores de «Mi alimentación» y a «¿Qué toca hoy?» de todos los usuarios, también sobre lo ya consumido en los últimos 7 días, porque se calculan al pedirlos.

### `DELETE /api/admin/grupos/{id}`
- **204:** borrado.
- **409 `GRUPO_EN_USO`:** tiene ingredientes; antes hay que cambiarlos de grupo.
- **404:** no existe.

### `POST /api/admin/ingredientes/{id}/fusionar`
```json
{ "destino": 26 }
```
Fusiona un ingrediente duplicado (`id`) con otro (`destino`) y borra el duplicado (D-065):
- Las recetas que llevaban el duplicado pasan a llevar el destino, también las eliminadas.
- Si una receta llevaba los dos, se suman sus cantidades en el texto («200 gramos + 1 unidad», hasta 100 caracteres).
- Si los grupos son distintos, esas recetas pasan a contar en el grupo del destino, también en los consumos ya registrados.
- El destino hereda el alimento de BEDCA del duplicado si no tenía uno, y los pesos de sus piezas que no tenga (D-080).
- **200:** `{ "ingrediente": { …el destino… }, "recetas": 3 }`, con cuántas recetas llevaban el duplicado.
- **400** con `errores.destino` si `destino` es el mismo ingrediente; **404** si alguno no existe.

### `DELETE /api/admin/ingredientes/{id}`
Para borrar ingredientes creados por error.
- **204:** borrado.
- **409 `INGREDIENTE_EN_USO`:** lo usa alguna receta (también una eliminada); en ese caso se fusiona con otro.
- **404:** no existe.

### `GET /api/admin/alimentos-bedca?q=`
Alimentos de BEDCA para asignar a un ingrediente (D-080): hasta 10 cuyo nombre contiene **todas** las palabras de `q`, en cualquier orden y sin distinguir mayúsculas ni acentos; primero los que empiezan por `q`. Sin texto, `[]`.

```json
[ { "id": 2292, "nombre": "Pollo, entero, con piel, crudo", "energiaKcal": 167, "energiaKj": 696.7,
    "grasas": 9.25, "grasasSaturadas": 2.66, "hidratos": 0, "fibra": 0, "proteinas": 20.85, "sodioMg": 63 } ]
```

- Valores por 100 g de porción comestible, los de BEDCA sin cambios; `null` si BEDCA no da ese valor. `energiaKcal` se calcula con la de kJ (÷ 4,184) y se redondea.

### `GET /api/admin/ingredientes/{id}/nutricion`
Los datos nutricionales del ingrediente, para editarlos (D-080, D-081): su alimento de BEDCA, con los valores por 100 g como en la búsqueda (o `null`), y los pesos de sus piezas, por orden alfabético.

```json
{ "alimento": { "id": 2421, "nombre": "Tomate", "energiaKcal": 19, "energiaKj": 78.5, "grasas": 0.1,
                "grasasSaturadas": null, "hidratos": 3.5, "fibra": 1.1, "proteinas": 0.9, "sodioMg": 18 },
  "piezas": [ { "pieza": "unidad", "gramos": 120.0 } ] }
```

- **404** si el ingrediente no existe.

### `PUT /api/admin/ingredientes/{id}/nutricion`
```json
{ "alimento": 2133, "piezas": [ { "pieza": "lata", "gramos": 60 } ] }
```
Sustituye el alimento de BEDCA con el que se calcula la información nutricional del ingrediente (`null`: ninguno) y los pesos de sus piezas (una lista vacía las quita), en una transacción (D-080, D-081).
- `piezas`: obligatoria, como mucho 10. Cada `pieza`, una sola palabra de letras, que se guarda en minúsculas; `gramos`, más de 0 y hasta 10.000, con un decimal como mucho.
- **200:** el ingrediente, con su `alimento`.
- **400** con errores por campo: `errores.alimento` si ese alimento no está en BEDCA; `errores['piezas[n].pieza']` si la pieza está repetida (en singular o en plural) o es una unidad que ya se convierte sola («gramo», «cucharada»), o si no es una palabra; `errores['piezas[n].gramos']` si el peso no vale.
- **404** si el ingrediente no existe.

## Imágenes (pública)

### `GET /api/imagenes/{nombre}?ancho=400`
`ancho` es opcional y puede ser `400`, `800` o `1200` (otro valor responde **400**). Sirve la imagen reducida a ese ancho, en JPEG; si la imagen ya es más estrecha, la imagen tal cual (D-069).

Devuelve la imagen con su `Content-Type` y `Cache-Control: public, max-age=31536000, immutable`. No exige sesión, porque las etiquetas `<img>` no envían el token. Un nombre que no existe o que no es válido responde 404 (D-034).

## Salud (pública)

### `GET /api/health` · `GET /api/health/liveness` · `GET /api/health/readiness`
Para Docker, Kubernetes o un balanceador. Responden **200** `{"status":"UP"}` cuando la aplicación está bien y **503** con `DOWN` cuando no. No dan detalles de los componentes. `/api/health` comprueba también la base de datos. El correo no cuenta: un SMTP caído no deja la aplicación fuera de servicio.

## Fuera de `/api`

El mismo servidor sirve el frontend (el build de Angular va dentro del JAR, D-058):
- cualquier ruta que no empiece por `/api` y no sea un fichero (`/recetas/5`, `/mi-alimentacion`…) devuelve `index.html`, para que funcionen las rutas de Angular;
- un fichero que no existe responde 404;
- los ficheros con hash en el nombre (`main-…js`, `styles-…css`, `media/`) se sirven con `Cache-Control: max-age=31536000, public, immutable`; el resto, sin caché;
- `manifest.webmanifest` se sirve como `application/manifest+json`;
- las respuestas de más de 2 KB van comprimidas con gzip si el navegador lo admite.

Todas las respuestas, también las de la API, llevan las cabeceras de seguridad de D-060: Content-Security-Policy, Referrer-Policy, Permissions-Policy, Cross-Origin-Opener-Policy, X-Frame-Options, X-Content-Type-Options y, por HTTPS, Strict-Transport-Security.
