# Alimentarse · Toca — ¿Qué toca hoy?

Aplicación web para publicar y compartir recetas, registrar cuándo se consumen y recibir recomendaciones de qué cocinar hoy. Las recomendaciones hacen que la alimentación de los **últimos 7 días** respete las raciones semanales recomendadas para cada grupo de alimentos.

![Recorrido animado por Toca: registro e inicio de sesión, portada, recetas con filtros y detalle, alta de una receta, «¿Qué toca hoy?», «Mi alimentación», planificador y lista de la compra, perfil, vista móvil y administración](demo.webp)

*Recorrido por toda la aplicación (11 min 41 s, con subtítulos y sin sonido). La guía de uso, pantalla a pantalla, está en [docs/GUIA.md](docs/GUIA.md).*

## Funcionalidades

- **Cuentas:**
  - registro con correo de bienvenida e inicio de sesión con nombre de usuario o email;
  - confirmación del email con un enlace, al registrarse y al cambiarlo;
  - acceso con Google, si se configuran sus credenciales: vincula la cuenta con el mismo email (con su contraseña, una vez) o crea una nueva;
  - recuperación de contraseña por email (enlace de un solo uso, válido 1 hora) y perfil editable;
  - descarga de todos los datos personales y baja de la cuenta, con confirmación por correo.
- **Recetas:**
  - alta y edición con editor de texto enriquecido, galería de hasta 6 fotos, enlace a un vídeo e ingredientes con cantidades;
  - publicar, despublicar y eliminar;
  - «me gusta» y la lista «Recetas que me gustan»;
  - valoraciones de 1 a 5 estrellas con un comentario, y la nota media en cada receta;
  - información nutricional estimada por ración y de la receta entera, con los datos de BEDCA;
  - listado con filtros (título, tipo, autor, dificultad, comensales, tiempo e ingredientes) y buscador con autocompletado.
- **Ingredientes:** catálogo con filtros por nombre y grupo. Cualquier usuario puede crearlos; editarlos, fusionar los duplicados, borrarlos y asignarles su alimento de BEDCA y el peso de sus piezas es cosa de los administradores.
- **¿Qué toca hoy?:** recetas que no llevan ningún grupo de alimentos agotado esta semana, con las de los grupos menos consumidos primero y el motivo de cada una. Antes de consumir una receta, un aviso dice cómo quedarían las raciones.
- **Planificador semanal:** comida y cena de los próximos días, con aviso si algo superaría su límite y los círculos de cómo quedaría la semana. De lo planificado sale la **lista de la compra**, por grupo de alimentos y con las cantidades sumadas cuando se puede.
- **Imprimir o guardar en PDF** una receta o la lista de la compra, desde el navegador.
- **Avisos push:** el recordatorio de la comida y la cena planificadas, una hora antes, en los navegadores en los que se activa.
- **Mi alimentación:**
  - últimas recetas consumidas, recetas más consumidas e ingredientes más usados;
  - indicadores circulares de los últimos 7 días (verde, ámbar o rojo);
  - nutrientes de los últimos 7 días, con la media al día y su porcentaje de la ingesta de referencia;
  - histórico de consumos, editable.
- **Administración:**
  - búsqueda de usuarios, rol de administrador y bloqueo de cuentas;
  - moderación: ocultar recetas con un motivo que ve su autor;
  - grupos de alimentos y sus raciones semanales.
- **PWA:** instalable en escritorio y móvil, con consulta sin conexión de los datos ya vistos y aviso de versión nueva.
- **Interfaz** en español, accesible (Lighthouse 100 en Accesibilidad) y adaptada a móvil, tableta y escritorio.

La [guía de uso](docs/GUIA.md) explica todo esto para usuarios y administradores.

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 25, Spring Boot 4.1 (Web MVC, Security con JWT, Validation, Mail + Thymeleaf), Maven Wrapper |
| Persistencia | MySQL 8.4, MyBatis, Flyway |
| Frontend | Angular 22 (standalone, signals, zoneless), TypeScript estricto, Bootstrap 5.3 + ng-bootstrap, Font Awesome, animate.css, TipTap (editor), service worker de Angular (PWA) |
| Pruebas | JUnit 6 + MockMvc, Vitest, Playwright (e2e) |
| Calidad | Spotless (Palantir Java Format), JaCoCo (≥ 80 % en los servicios), cobertura de Vitest (≥ 85 % de líneas en el frontend), ESLint (angular-eslint), Prettier |
| Despliegue | JAR único con el frontend dentro, Docker (imagen en 3 etapas) y Docker Compose |
| Integración continua | GitHub Actions: backend, frontend, e2e y Docker en cada push y pull request |
| Dependencias | Dependabot: un pull request por grupo cada semana, comprobado por la CI ([D-072](docs/DECISIONES.md#d-072--actualizaciones-automáticas-de-dependencias)) |
| Correo en desarrollo | Mailpit |

## Requisitos

- **JDK 25** (por ejemplo, Eclipse Temurin). No hace falta instalar Maven: se usa el Maven Wrapper (`backend/mvnw.cmd`, o `backend/mvnw` en Linux y macOS).
- **Node.js 24** (24.15 o superior; también vale 22.22+) y npm 11. No hace falta para generar el JAR con el perfil `frontend`, que descarga su propio Node.
- **MySQL 8**: local, el portable de `scripts/` o en Docker.
- **Mailpit** para ver los correos en desarrollo: con el script incluido o en Docker.
- Opcional: **Docker** con Compose.

Los scripts de `scripts/` son de PowerShell (Windows). En Linux y macOS, MySQL y Mailpit se levantan con `docker compose up -d`.

## Puesta en marcha en desarrollo

1. **Variables de entorno.** Copia `.env.example` como `.env` y rellena las contraseñas (ver [Variables de entorno](#variables-de-entorno)). Spring Boot y Docker Compose leen ese fichero. Escribe los comentarios en líneas propias y no pongas comillas en los valores.
2. **Base de datos.** Elige una de estas opciones:
   - **Sin Docker, MySQL portable (recomendada en Windows).**
     - Rellena `DB_PASSWORD` y `MYSQL_ROOT_PASSWORD` en `.env` y pon `DB_URL=jdbc:mysql://localhost:3307/toca`.
     - Ejecuta `powershell -ExecutionPolicy Bypass -File scripts\mysql-portable.ps1`; no necesita permisos de administrador.
     - La primera vez descarga MySQL 8.4 LTS en `.tools/` y crea las bases de datos `toca` y `toca_test` y el usuario de `.env`. Después deja el servidor escuchando en `127.0.0.1:3307`; se para con Ctrl+C.
   - **Con un MySQL 8 ya instalado.** Con el servicio arrancado, ejecuta `powershell -ExecutionPolicy Bypass -File scripts\mysql-init-local.ps1`. Pide la contraseña de root y crea lo mismo en el puerto 3306.
   - **Con Docker.** `docker compose up -d` levanta MySQL 8.4 (puerto 3306) y Mailpit, accesibles solo desde este equipo (`127.0.0.1`).
3. **Correo (si no usas Docker).** `powershell -ExecutionPolicy Bypass -File scripts\mailpit.ps1` deja el SMTP en el puerto 1025 y la interfaz web en http://localhost:8025.
4. **Backend.** `cd backend` y `.\mvnw.cmd spring-boot:run`. La API queda en http://localhost:8080/api. `spring-boot:run` arranca con el perfil `dev`, con Swagger UI en http://localhost:8080/api/docs.
5. **Frontend.** `cd frontend`, `npm install` y `npm start`. La aplicación queda en http://localhost:4200, y las peticiones a `/api` se redirigen al backend mediante `proxy.conf.json`.

### Usuarios de demostración (solo en el perfil `dev`)

Al arrancar en `dev` se cargan diez recetas originales y tres cuentas con recetas, «me gusta» e historial de consumos, que se regenera en cada arranque. La contraseña de las tres es `demo1234`.

| Usuario | Para qué sirve |
|---|---|
| `lucia` | Sus últimos 7 días reproducen el ejemplo de la especificación: Carne 3/3 (rojo), Pasta 3/4 (ámbar), Pescado 1/4 (verde), Fruta 3/∞ |
| `marcos` | Tiene agotado el grupo Dulces, así que «¿Qué toca hoy?» le oculta los rollos de canela. Es autor de una receta sin publicar |
| `carmen` | Casi todo su historial queda fuera de la ventana de 7 días |

En cualquier perfil, el primer arranque crea el administrador `admin` con la contraseña de `ADMIN_INITIAL_PASSWORD`. Si esa variable está vacía, no lo crea y lo avisa en el log.

## Variables de entorno

La aplicación lee las variables de entorno y, en desarrollo, también el fichero `.env` (en la raíz o en `backend/`). Una variable de entorno real tiene prioridad. [`.env.example`](.env.example) tiene todas, comentadas.

| Variable | Obligatoria | Por defecto | Para qué |
|---|---|---|---|
| `DB_URL` | sí | — | JDBC de MySQL, por ejemplo `jdbc:mysql://localhost:3306/toca` |
| `DB_USER`, `DB_PASSWORD` | sí | — | Usuario de la base de datos |
| `JWT_SECRET` | sí | — | Secreto de firma de los tokens, de al menos 256 bits (`openssl rand -base64 48`) |
| `JWT_EXPIRATION_HOURS` | no | `8` | Duración de la sesión |
| `COOKIE_SEGURA` | no | `true` (`false` en `dev` y en Docker Compose) | La cookie de sesión solo viaja por HTTPS |
| `PROXIES_DE_CONFIANZA` | no | `127.0.0.0/8, ::1/128` (vacía en Docker Compose) | Solo en `prod`: rangos CIDR, separados por comas, de los proxies cuyas cabeceras `X-Forwarded-*` se creen. Vacía, ninguno ([D-090](docs/DECISIONES.md#d-090--tercera-revisión-de-seguridad)) |
| `MAIL_HOST`, `MAIL_PORT` | no | `localhost`, `1025` | Servidor SMTP |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | no | vacías | Credenciales SMTP, si el servidor las pide |
| `MAIL_FROM` | no | `no-reply@toca.local` | Remitente de los correos |
| `APP_BASE_URL` | no | `http://localhost:4200` | URL pública del frontend, que va en los enlaces de los correos. Con el JAR, `http://localhost:8080`; en producción, la URL `https://` |
| `UPLOAD_DIR` | no | `./uploads` | Carpeta de las imágenes subidas |
| `ADMIN_INITIAL_PASSWORD` | en el primer arranque | — | Contraseña del administrador `admin` que se crea si no hay ninguno (mínimo 8 caracteres) |
| `ADMIN_INITIAL_EMAIL` | no | `admin@toca.local` | Email de ese administrador |
| `PUSH_CLAVE_PUBLICA`, `PUSH_CLAVE_PRIVADA` | no | vacías | Claves VAPID de los avisos push; sin ellas, no hay avisos. Se generan con `scripts\generar-claves-push.ps1` |
| `PUSH_CONTACTO` | no | `mailto:no-reply@toca.local` | Contacto que se da al servicio push (`mailto:` o `https:`) |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | no | vacías | Cliente OAuth de Google Cloud Console para entrar con Google; sin ellas, no hay botón (D-085) |
| `SPRING_PROFILES_ACTIVE` | sí, salvo con `mvnw spring-boot:run` | — | `dev` (datos de demostración y Swagger) o `prod`. Sin ella, el JAR no arranca (D-089). Solo funciona como variable de entorno real |
| `TEST_DB_URL` | no | `jdbc:mysql://localhost:3307/toca_test` | Base de datos de los tests sin Docker y de las pruebas e2e. **Se borra en cada ejecución** |
| `MYSQL_ROOT_PASSWORD` | con Docker o el MySQL portable | — | Contraseña de root de ese MySQL. La aplicación no la usa |
| `APP_BASE_URL_DOCKER`, `SPRING_PROFILES_ACTIVE_DOCKER` | no | `http://localhost:8080`, `prod` | Valores para el perfil `full` de Docker Compose |

Las pruebas e2e admiten además `E2E_URL`, `E2E_DB_URL`, `E2E_PUERTO`, `E2E_NAVEGADOR` y `E2E_ADMIN_PASSWORD` (ver `frontend/playwright.config.ts`).

## Tests y calidad

> Los tests del backend necesitan MySQL: con Docker usan Testcontainers; sin Docker, `TEST_DB_URL` (por defecto `toca_test`), que **se borra en cada ejecución**.

| Qué | Comando |
|---|---|
| Build y tests del backend, con formato y cobertura | `cd backend; .\mvnw.cmd verify` |
| Formatear el código Java | `cd backend; .\mvnw.cmd spotless:apply` |
| Tests del frontend (Vitest) | `cd frontend; npm test` |
| Tests del frontend con cobertura mínima | `npm run test:cobertura` (umbrales en `angular.json`, informe en `frontend/coverage`) |
| Lint y formato del frontend | `npm run lint`, `npm run format:check` (corregir: `npm run format`) |
| Pruebas e2e (Playwright) | `cd frontend; npm run e2e`. Necesita el JAR y MySQL; arranca la aplicación en el puerto 8081 contra `toca_test`, que vacía |
| Tipos de la API en el frontend, tras cambiar un DTO o un endpoint | `cd backend; .\mvnw.cmd test "-Dtest=OpenApiTest"` (regenera `docs/openapi.json`) y después `cd frontend; npm run api` ([D-073](docs/DECISIONES.md#d-073--tipos-del-frontend-generados-desde-el-openapi)) |

**Pruebas e2e.** Recorren:
- registro → login → crear receta → publicar → «me gusta» → consumir → Mi alimentación → la recomendación excluye el grupo agotado;
- el administrador da el rol de administrador y edita un ingrediente;
- el administrador oculta una receta y bloquea a su autor, cuya sesión se cierra al momento;
- un usuario da «me gusta», elimina su receta, descarga sus datos y se da de baja;
- un usuario planifica comidas, ve cómo quedaría la semana, quita una y marca otra como comida;
- ninguna pantalla tiene scroll horizontal a 375, 768 y 1280 px;
- axe-core no encuentra infracciones de WCAG 2.2 A y AA en ninguna pantalla, con modales abiertos, errores de formulario y en móvil;
- la aplicación se usa solo con el teclado, y el foco pasa al título de cada pantalla nueva.

El informe, con capturas de cada pantalla, queda en `frontend/playwright-report`. En Windows usan el Edge instalado; en otros sistemas, instala antes Chromium con `npx playwright install chromium`.

**Integración continua** (`.github/workflows/ci.yml`). En cada push a `main` y en cada pull request se pasan:
- los tests del backend, con MySQL de Testcontainers;
- lint, formato, tests y build del frontend;
- las pruebas e2e con Chromium;
- la construcción de la imagen Docker, levantando `docker compose --profile full` y comprobando que la aplicación es usable.

**Lighthouse** (build de producción, escritorio y móvil, las 20 pantallas):
- Accesibilidad: 100 en todas.
- Buenas prácticas: 96–100.
- SEO: 100.
- Rendimiento: 100.
- Instalable como PWA según el navegador.

Los detalles están en [D-059](docs/DECISIONES.md#d-059--lighthouse-y-revisión-final) y [D-087](docs/DECISIONES.md#d-087--bloque-de-cierre-criterios-de-aceptación-lighthouse-y-dependencias).

## Build y despliegue

### JAR único

```powershell
cd backend
.\mvnw.cmd -Pfrontend package -DskipTests      # → target/alimentarse.jar (API + frontend)
$env:SPRING_PROFILES_ACTIVE = 'prod'
java -jar target/alimentarse.jar
```

- **Qué hace el perfil `frontend`:** descarga Node en `backend/target/node`, ejecuta `npm ci` y `npm run build`, y mete el build de Angular en el JAR.
- **Rutas de Angular:** funcionan al recargar o al abrir un enlace directo, porque el backend responde `index.html` a todo lo que no es `/api` ni un fichero.
- **Caché:** los ficheros con hash en el nombre se cachean un año; `index.html`, `ngsw.json` y el resto, no.

### Docker

```bash
docker build -t alimentarse .                 # imagen de la aplicación
docker compose --profile full up -d --build   # aplicación + MySQL + Mailpit → http://localhost:8080
```

- **Imagen:** el `Dockerfile` construye el frontend con Node 24 y el JAR con Temurin 25, y deja una imagen final solo con el JRE. Se ejecuta con un usuario sin privilegios y guarda las imágenes subidas en el volumen `/app/uploads`. Las imágenes base van fijadas por digest, y Dependabot propone cada semana las que traen parches (D-090).
- **Perfil `full`:**
  - Toma del `.env` solo las variables que usa la aplicación, listadas en `docker-compose.yml` (no le llega `MYSQL_ROOT_PASSWORD`), y apunta `DB_URL` y `MAIL_HOST` a los servicios de Compose.
  - Escucha solo en `127.0.0.1:8080`, por HTTP y sin proxies de confianza (D-090).
  - Para servirla a otros equipos, pon delante un proxy con HTTPS. Si está en el mismo equipo, la aplicación lo ve llegar desde la pasarela de Docker: `PROXIES_DE_CONFIANZA` debe ser la red de Compose (por ejemplo `172.16.0.0/12`).
- **Verificado en la integración continua:** se construye la imagen, se levanta el perfil `full` y se comprueba que la aplicación es usable (D-060).

### En producción

- **HTTPS es obligatorio.** El service worker de la PWA solo funciona en un contexto seguro.
  - Si un proxy o PaaS termina el TLS, el perfil `prod` respeta las cabeceras `X-Forwarded-*` (`server.forward-headers-strategy: native`), pero solo si llegan de un proxy de confianza: por defecto, solo esta máquina (D-090).
  - **Si el proxy está en otra máquina, pon sus rangos en `PROXIES_DE_CONFIANZA`** (por ejemplo `10.0.0.0/8`, o el que indique tu PaaS). Si no, la aplicación verá la IP del proxy: los límites de intentos se compartirán entre todos los usuarios, y sin `X-Forwarded-Proto` no se enviará HSTS ni se construirá bien la vuelta del acceso con Google.
  - No pongas un rango desde el que también puedan llegar clientes sin pasar por el proxy: cambiando `X-Forwarded-For`, se saltarían los límites de intentos.
  - El proxy debe redirigir HTTP a HTTPS y no cachear `index.html` ni `ngsw.json`.
- **Variables:**
  - `SPRING_PROFILES_ACTIVE=prod`;
  - un `JWT_SECRET` propio y largo;
  - `COOKIE_SEGURA=true` (el valor por defecto) si se sirve por HTTPS, o la sesión no funcionará sin él;
  - `APP_BASE_URL` con la URL `https://` pública;
  - un SMTP real en `MAIL_*`;
  - `UPLOAD_DIR` en un almacenamiento persistente;
  - para entrar con Google, `GOOGLE_CLIENT_ID` y `GOOGLE_CLIENT_SECRET`, con `https://{tu dominio}/login/oauth2/code/google` entre los URI de redirección autorizados del cliente (D-085).
- **Primer arranque:** crea `admin` con `ADMIN_INITIAL_PASSWORD`. Cambia esa contraseña desde «Mi perfil» y nombra otros administradores desde la aplicación.
- **Sin extras de desarrollo:** en `prod` no hay datos de demostración ni Swagger UI.
- **Salud:** `GET /api/health/readiness` (público) para el balanceador o el orquestador. La imagen Docker ya lo usa en su `HEALTHCHECK`.
- **Ya configurado:** cabeceras de seguridad (Content-Security-Policy estricta para scripts, HSTS por HTTPS, Referrer-Policy, Permissions-Policy) y compresión gzip (D-060). Si añades recursos de otros dominios (fuentes, analítica…), amplía la CSP en `SecurityConfig`.
- **Copias de seguridad:** la base de datos y la carpeta de imágenes.

## Estructura

```
backend/     Spring Boot: API REST y, empaquetado, también el frontend
frontend/    Angular; pruebas e2e en frontend/e2e
scripts/     desarrollo sin Docker (MySQL portable o local, Mailpit) y generación de iconos e imágenes demo
docs/        GUIA.md, API.md, DECISIONES.md, openapi.json (descripción de la API) e imágenes de la guía
Dockerfile, docker-compose.yml
demo.webp    recorrido animado de la aplicación (el del principio de este README)
```

## Documentación

- [docs/GUIA.md](docs/GUIA.md): guía de uso para usuarios y administradores.
- [docs/API.md](docs/API.md): API REST. En `dev`, también Swagger UI en http://localhost:8080/api/docs.
- [docs/openapi.json](docs/openapi.json): descripción OpenAPI 3.1 de la API, de la que se generan los tipos del frontend.
- [docs/DECISIONES.md](docs/DECISIONES.md): decisiones tomadas y desviaciones respecto a la especificación.

## Contenido

- **Material propio:** las recetas de ejemplo, sus imágenes (placeholders generados con `scripts/generar-imagenes-demo.ps1`), la ilustración de la portada, los iconos de la aplicación y el recorrido animado (`demo.webp`, grabado de la aplicación con los datos de demostración) son originales.
- **Composición de los alimentos:** la información nutricional usa los valores, sin cambios, de los 957 alimentos de la AESAN/BEDCA Base de Datos Española de Composición de Alimentos v1.0 (2010) ([bedca.net](https://www.bedca.net/)), con fines educativos y no comerciales y citando la fuente (D-079). Para un uso comercial habría que pedir autorización a BEDCA.
- **Librerías:** se usan con sus licencias. Por ejemplo, los iconos de la interfaz son de Font Awesome Free (CC BY 4.0).
