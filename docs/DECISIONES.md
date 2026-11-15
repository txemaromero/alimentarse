# Decisiones y desviaciones

Registro de las decisiones que no están fijadas en la especificación del proyecto o que se apartan de ella. Las más recientes van al final.

## Índice

| Decisión | Título |
|---|---|
| [D-001](#d-001--spring-boot-412-en-lugar-de-3x) | Spring Boot 4.1.2 en lugar de 3.x |
| [D-002](#d-002--java-25-en-lugar-de-java-21) | Java 25 en lugar de Java 21 |
| [D-003](#d-003--vitest-para-los-tests-unitarios-de-angular) | Vitest para los tests unitarios de Angular |
| [D-004](#d-004--desarrollo-sin-docker-en-esta-máquina) | Desarrollo sin Docker en esta máquina |
| [D-005](#d-005--mysql84-en-docker-compose) | `mysql:8.4` en Docker Compose |
| [D-006](#d-006--el-env-lo-lee-también-spring-boot) | El `.env` lo lee también Spring Boot |
| [D-007](#d-007--nombres-paquete-esalimentarse-y-base-de-datos-qch) | Nombres: paquete `es.alimentarse` y base de datos `qch` |
| [D-008](#d-008--palantir-java-format-con-spotless) | Palantir Java Format con Spotless |
| [D-009](#d-009--la-memoria-en-pdf-no-se-versiona) | La memoria en PDF no se versiona |
| [D-010](#d-010--las-dependencias-se-añaden-en-la-fase-que-las-usa) | Las dependencias se añaden en la fase que las usa |
| [D-011](#d-011--cobertura-medida-sobre-las-clases-service) | Cobertura medida sobre las clases `*Service` |
| [D-012](#d-012--mysql-84-portable-para-desarrollar-sin-docker) | MySQL 8.4 portable para desarrollar sin Docker |
| [D-013](#d-013--fechas-instant-en-java-y-utc-en-la-base-de-datos) | Fechas: `Instant` en Java y UTC en la base de datos |
| [D-014](#d-014--desviaciones-del-modelo-de-datos-respecto-a-la-memoria) | Desviaciones del modelo de datos respecto a la memoria |
| [D-015](#d-015--datos-de-demostración-migración-repetible-y-callback-de-flyway) | Datos de demostración: migración repetible y callback de Flyway |
| [D-016](#d-016--tests-de-integración-con-o-sin-docker) | Tests de integración con o sin Docker |
| [D-017](#d-017--mybatis-añadido-a-mano-al-pomxml) | MyBatis añadido a mano al `pom.xml` |
| [D-018](#d-018--jjwt-con-gson-para-el-json-de-los-tokens) | jjwt con Gson para el JSON de los tokens |
| [D-019](#d-019--los-roles-viajan-en-el-token) | Los roles viajan en el token |
| [D-020](#d-020--correos-asíncronos-tras-el-commit) | Correos asíncronos tras el commit |
| [D-021](#d-021--he-olvidado-mi-contraseña-no-revela-si-el-email-existe) | «He olvidado mi contraseña» no revela si el email existe |
| [D-022](#d-022--rate-limiting-en-memoria-por-ip) | Rate limiting en memoria por IP |
| [D-023](#d-023--email-del-administrador-inicial-configurable) | Email del administrador inicial configurable |
| [D-024](#d-024--contraseñas-de-8-a-72-caracteres) | Contraseñas de 8 a 72 caracteres |
| [D-025](#d-025--logs-de-sql-desactivados-en-dev) | Logs de SQL desactivados en `dev` |
| [D-026](#d-026--paginación-página-desde-0-y-como-máximo-100-elementos) | Paginación: página desde 0 y como máximo 100 elementos |
| [D-027](#d-027--búsquedas-de-texto-contiene-sin-mayúsculas-ni-acentos-y-sin-comodines) | Búsquedas de texto: «contiene» sin mayúsculas ni acentos y sin comodines |
| [D-028](#d-028--sugerencias-de-ingredientes) | Sugerencias de ingredientes |
| [D-029](#d-029--los-grupos-de-alimentos-no-se-editan-desde-la-api) | Los grupos de alimentos no se editan desde la API |
| [D-030](#d-030--ids-generados-con-last_insert_id) | Ids generados con `LAST_INSERT_ID()` |
| [D-031](#d-031--orden-del-listado-de-recetas) | Orden del listado de recetas |
| [D-032](#d-032--visibilidad-y-permisos-de-las-recetas) | Visibilidad y permisos de las recetas |
| [D-033](#d-033--me-gusta-idempotentes-y-solo-en-recetas-publicadas) | «Me gusta» idempotentes y solo en recetas publicadas |
| [D-034](#d-034--imágenes-de-las-recetas) | Imágenes de las recetas |
| [D-035](#d-035--saneado-del-html-de-las-instrucciones) | Saneado del HTML de las instrucciones |
| [D-036](#d-036--alta-y-edición-de-recetas-en-multipart) | Alta y edición de recetas en multipart |
| [D-037](#d-037--límites-de-validación-de-las-recetas) | Límites de validación de las recetas |
| [D-038](#d-038--ventana-de-los-últimos-7-días) | Ventana de los «últimos 7 días» |
| [D-039](#d-039--reglas-de-los-consumos) | Reglas de los consumos |
| [D-040](#d-040--resumen-de-mi-alimentación) | Resumen de «Mi alimentación» |
| [D-041](#d-041--recomendación-qué-toca-hoy) | Recomendación «¿Qué toca hoy?» |
| [D-042](#d-042--columnas-de-la-tarjeta-de-receta-reutilizables-con-un-prefijo) | Columnas de la tarjeta de receta reutilizables con un prefijo |
| [D-043](#d-043--administración-rol-comprobado-en-la-base-de-datos-y-cambios-serializados) | Administración: rol comprobado en la base de datos y cambios serializados |
| [D-044](#d-044--búsqueda-de-usuarios-en-la-administración) | Búsqueda de usuarios en la administración |
| [D-045](#d-045--qué-pantallas-son-públicas) | Qué pantallas son públicas |
| [D-046](#d-046--sesión-del-frontend-en-localstorage) | Sesión del frontend en localStorage |
| [D-047](#d-047--tratamiento-de-errores-en-el-frontend) | Tratamiento de errores en el frontend |
| [D-048](#d-048--angular-22-onpush-por-defecto-formularios-reactivos-y-textos-de-ng-bootstrap) | Angular 22: OnPush por defecto, formularios reactivos y textos de ng-bootstrap |
| [D-049](#d-049--presupuesto-del-bundle-inicial) | Presupuesto del bundle inicial |
| [D-050](#d-050--editor-de-las-instrucciones-tiptap) | Editor de las instrucciones: TipTap |
| [D-051](#d-051--estado-de-los-listados-en-memoria-no-en-la-url) | Estado de los listados en memoria, no en la URL |
| [D-052](#d-052--añadir-a-consumidos-desde-el-detalle-fase-8) | «Añadir a consumidos» desde el detalle (fase 8) |
| [D-053](#d-053--detalles-de-la-interfaz-de-recetas-e-ingredientes) | Detalles de la interfaz de recetas e ingredientes |
| [D-054](#d-054--ingredientes-más-usados-sin-los-grupos-básicos) | «Ingredientes más usados» sin los grupos básicos |
| [D-055](#d-055--pantallas-de-alimentación) | Pantallas de alimentación |
| [D-056](#d-056--perfil-y-gestión-de-usuarios-en-el-frontend) | Perfil y gestión de usuarios en el frontend |
| [D-057](#d-057--pwa-iconos-caché-sin-conexión-y-privacidad) | PWA: iconos, caché sin conexión y privacidad |
| [D-058](#d-058--empaquetado-rutas-de-angular-y-pruebas-e2e) | Empaquetado, rutas de Angular y pruebas e2e |
| [D-059](#d-059--lighthouse-y-revisión-final) | Lighthouse y revisión final |
| [D-060](#d-060--integración-continua-cabeceras-de-seguridad-compresión-y-salud) | Integración continua, cabeceras de seguridad, compresión y salud |
| [D-061](#d-061--eliminar-una-receta-propia) | Eliminar una receta propia |
| [D-062](#d-062--recetas-que-me-gustan) | «Recetas que me gustan» |
| [D-063](#d-063--moderación-recetas-ocultas-y-usuarios-bloqueados) | Moderación: recetas ocultas y usuarios bloqueados |
| [D-064](#d-064--descargar-mis-datos-y-darse-de-baja) | Descargar mis datos y darse de baja |
| [D-065](#d-065--grupos-de-alimentos-e-ingredientes-desde-la-administración) | Grupos de alimentos e ingredientes desde la administración |
| [D-066](#d-066--marca-visible-toca--qué-toca-hoy) | Marca visible: «Toca — ¿Qué toca hoy?» |
| [D-067](#d-067--recomendación-variedad-motivo-y-aviso-antes-de-comer) | Recomendación: variedad, motivo y aviso antes de comer |
| [D-068](#d-068--planificador-semanal) | Planificador semanal |
| [D-069](#d-069--imágenes-recodificadas-y-en-varios-tamaños) | Imágenes recodificadas y en varios tamaños |
| [D-070](#d-070--sesión-en-cookie-httponly-y-anulación-de-sesiones) | Sesión en cookie HttpOnly y anulación de sesiones |
| [D-071](#d-071--límite-de-intentos-compartido-entre-servidores) | Límite de intentos compartido entre servidores |
| [D-072](#d-072--actualizaciones-automáticas-de-dependencias) | Actualizaciones automáticas de dependencias |
| [D-073](#d-073--tipos-del-frontend-generados-desde-el-openapi) | Tipos del frontend generados desde el OpenAPI |
| [D-074](#d-074--cobertura-mínima-de-los-tests-del-frontend) | Cobertura mínima de los tests del frontend |
| [D-075](#d-075--accesibilidad-pruebas-automáticas-foco-al-cambiar-de-pantalla-y-revisión-con-nvda) | Accesibilidad: pruebas automáticas, foco al cambiar de pantalla y revisión con NVDA |
| [D-076](#d-076--lista-de-la-compra-e-impresión-en-pdf) | Lista de la compra e impresión en PDF |
| [D-077](#d-077--valoraciones-con-nota-y-comentarios) | Valoraciones con nota y comentarios |
| [D-078](#d-078--avisos-push-del-planificador) | Avisos push del planificador |
| [D-079](#d-079--estudio-nutricional-con-bedca) | Estudio nutricional con BEDCA |
| [D-080](#d-080--catálogo-de-bedca-y-alimentos-asignados-por-la-administración) | Catálogo de BEDCA y alimentos asignados por la administración |
| [D-081](#d-081--pesos-de-las-piezas-desde-la-administración-y-números-con-letras) | Pesos de las piezas desde la administración y números con letras |
| [D-082](#d-082--nutrientes-de-la-semana-en-mi-alimentación) | Nutrientes de la semana en «Mi alimentación» |
| [D-083](#d-083--galería-de-fotos-y-enlace-a-un-vídeo-en-las-recetas) | Galería de fotos y enlace a un vídeo en las recetas |
| [D-084](#d-084--alimento-de-bedca-para-la-carrillada) | Alimento de BEDCA para la Carrillada |
| [D-085](#d-085--acceso-con-google-openid-connect) | Acceso con Google (OpenID Connect) |
| [D-086](#d-086--revisión-de-seguridad-del-cierre) | Revisión de seguridad del cierre |
| [D-087](#d-087--bloque-de-cierre-criterios-de-aceptación-lighthouse-y-dependencias) | Bloque de cierre: criterios de aceptación, Lighthouse y dependencias |
| [D-088](#d-088--confirmación-del-email) | Confirmación del email |
| [D-089](#d-089--segunda-revisión-de-seguridad) | Segunda revisión de seguridad |
| [D-090](#d-090--tercera-revisión-de-seguridad) | Tercera revisión de seguridad |
| [D-091](#d-091--contorno-de-los-campos-de-formulario) | Contorno de los campos de formulario |


## D-001 · Spring Boot 4.1.2 en lugar de 3.x
- **Fase:** 0 · **Decidido por:** usuario
- **Motivo:** la especificación pide «Spring Boot 3.x», pero también «la última versión estable». La rama 3.5.x dejó de tener soporte OSS el 30/06/2026 y Spring Initializr ya no la ofrece. La 4.1.x tiene soporte OSS hasta el 31/07/2027.
- **Consecuencias:**
  - Se usan los starters de Boot 4: `spring-boot-starter-webmvc` y los `*-test` por módulo.
  - Las dependencias gestionadas pasan a ser Jackson 3, JUnit 6, Flyway 12 y Testcontainers 2.
  - Compatibilidad comprobada: `mybatis-spring-boot-starter` 4.1.0 y `springdoc-openapi` 3.2.0.

## D-002 · Java 25 en lugar de Java 21
- **Fase:** 0 · **Decidido por:** usuario
- **Motivo:** Java 25 es la LTS más reciente (Temurin 25.0.5) y Spring Boot 4.1 la soporta.
- **Consecuencias:** imagen base `eclipse-temurin:25` en el Dockerfile (fase 12).

## D-003 · Vitest para los tests unitarios de Angular
- **Fase:** 0
- **Motivo:** la especificación propone «Jasmine/Karma o Jest». Angular CLI 22 genera los proyectos con **Vitest** por defecto (builder `@angular/build:unit-test` con jsdom), y Karma está obsoleto. Es la opción más simple y la oficial.
- **Consecuencias:** `npm test` ejecuta `ng test --watch=false`.

## D-004 · Desarrollo sin Docker en esta máquina
- **Fase:** 0 · **Decidido por:** usuario
- **Motivo:** Docker no está instalado. Hay un MySQL Server 8.0.25 local.
- **Consecuencias:**
  - En esta máquina se usa MySQL 8.4 portable (ver D-012).
  - `scripts/mysql-init-local.ps1` sigue disponible para quien tenga un MySQL 8 instalado: crea `toca`, `toca_test` y el usuario, con la contraseña tomada de `.env`.
  - `scripts/mailpit.ps1` descarga la versión de Mailpit fijada en el script, verifica su SHA-256 y lo arranca.
  - `docker-compose.yml` existe, pero no se ha podido validar aquí.
  - Los tests de integración tendrán que funcionar sin Testcontainers (fase 1).

## D-005 · `mysql:8.4` en Docker Compose
- **Fase:** 0
- **Motivo:** la especificación dice «MySQL 8». La 8.4 es la LTS vigente de la serie 8 (la 8.0 llegó a fin de vida en 04/2026).
- **Consecuencias:** se configura con `utf8mb4` y `utf8mb4_0900_ai_ci`, la misma colación que crea el script local.

## D-006 · El `.env` lo lee también Spring Boot
- **Fase:** 0
- **Motivo:** para que `mvnw spring-boot:run` funcione sin configurar variables a mano.
- **Detalle:** `spring.config.import: optional:file:.env[.properties],optional:file:../.env[.properties]`.
- **Consecuencias:**
  - El `.env` debe ser compatible con el formato `.properties`: sin comentarios al final de línea, sin comillas y con `/` en las rutas.
  - Las variables de entorno reales tienen prioridad sobre el `.env`.
  - `SPRING_PROFILES_ACTIVE` solo surte efecto como variable de entorno real; si no está, el perfil es `dev` (`spring.profiles.default`).

## D-007 · Nombres: paquete `es.alimentarse` y base de datos `qch`
- **Fase:** 0
- **Motivo:** la aplicación se llama Alimentarse y la marca visible es «QCH — ¿Qué como hoy?». La base de datos usa el nombre `qch`, como en las variables de entorno de la especificación.
- **Consecuencias:** `groupId`/`artifactId` = `es.alimentarse` / `alimentarse`.
- **Sustituida en parte por D-066:** la marca visible es «Toca — ¿Qué toca hoy?» y los nombres técnicos usan `toca` en lugar de `qch` (bases de datos `toca` y `toca_test`, usuario `toca`). El paquete sigue siendo `es.alimentarse`.

## D-008 · Palantir Java Format con Spotless
- **Fase:** 0
- **Motivo:** la especificación pide «Spotless o similar». Palantir es una variante de google-java-format con 4 espacios y 120 columnas, más legible con nombres de dominio largos.
- **Consecuencias:** `spotless:check` se ejecuta en `verify`.

## D-009 · La memoria en PDF no se versiona
- **Fase:** 0
- **Motivo:** contiene material de terceros (recetas de Nestlé Cocina) y ocupa 3 MB. Se queda en la raíz como referencia local, ignorada por git.

## D-010 · Las dependencias se añaden en la fase que las usa
- **Fase:** 0
- **Motivo:** así cada fase arranca y se prueba de forma aislada. En la fase 0 el contexto de Spring arranca sin base de datos.
- **Consecuencias:**
  - Fase 1: MyBatis, Flyway y el driver de MySQL.
  - Fase 2: JWT y springdoc.
  - Fase 4: el sanitizador de HTML.
  - Fase 7: las librerías de interfaz.

## D-011 · Cobertura medida sobre las clases `*Service`
- **Fase:** 0
- **Motivo:** el criterio de aceptación es «cobertura del backend ≥ 80 % en servicios».
- **Consecuencias:** la regla de JaCoCo exige un 80 % de líneas cubiertas al conjunto de clases cuyo nombre acaba en `Service`. Por eso la lógica de negocio debe vivir en clases con ese sufijo.

## D-012 · MySQL 8.4 portable para desarrollar sin Docker
- **Fase:** 0 (preparación de la fase 1) · **Decidido por:** usuario
- **Motivo:**
  - El MySQL Server 8.0.25 instalado como servicio (`MySQL80`) no arranca y no deja rastro: ni log de errores (el último es de 12/2024), ni eventos del sistema, y termina con código 0. Además, inicializarlo requería la contraseña de root del usuario.
  - La 8.0 llegó a fin de vida en 04/2026 y Flyway Community podría rechazarla.
- **Decisión:** `scripts/mysql-portable.ps1`:
  - Descarga el ZIP oficial de MySQL **8.4.13** LTS desde `cdn.mysql.com` a `.tools/mysql/`.
  - Inicializa los datos con `--initialize-insecure --init-file`, de modo que la contraseña de root sale de `MYSQL_ROOT_PASSWORD` y se crean `toca`, `toca_test` y el usuario de `.env`.
  - Arranca el servidor como proceso del usuario, sin servicio ni administrador, en `127.0.0.1:3307`, con mysqlx desactivado y `utf8mb4_0900_ai_ci`.
- **Puerto 3307:** para no chocar con el 3306 si `MySQL80` (arranque automático) volviera a funcionar. En el `.env` local, `DB_URL=jdbc:mysql://localhost:3307/toca`.
- **Integridad de la descarga:**
  - dev.mysql.com no permite consultar el MD5 oficial desde scripts.
  - El SHA-256 se fijó en el script tras la primera descarga por HTTPS desde el CDN oficial: las siguientes descargas se verifican contra ese mismo fichero.
  - El MD5 del ZIP (`7b94489d5524dea9b27097068bea25a8`) puede compararse a mano con el de la página oficial de descargas de MySQL 8.4.
- **No se toca** la instalación `MySQL80` ni sus bases de datos.

## D-013 · Fechas: `Instant` en Java y UTC en la base de datos
- **Fase:** 1
- **Motivo:** la ventana de 7 días (6.2) y el histórico de consumos deben calcularse igual sea cual sea la zona horaria del servidor o del contenedor.
- **Decisión:**
  - Las columnas son `TIMESTAMP`, como en la especificación.
  - El driver abre la sesión en UTC (`connectionTimeZone=UTC` y `forceConnectionTimeZoneToSession=true`, configurados en `spring.datasource.hikari.data-source-properties`), así que `NOW()` y los `TIMESTAMP` van en UTC.
  - En Java las fechas son `Instant`.
  - El frontend se encarga de mostrarlas en la hora local del usuario.

## D-014 · Desviaciones del modelo de datos respecto a la memoria
- **Fase:** 1
- **Columnas añadidas** (marcadas `[añadido]` en la sección 5):
  - `usuarios.TOKEN_EXPIRACION`: el enlace de recuperación de contraseña caduca a la hora.
  - `usuario_receta_consumida.ID`: permite editar o borrar un consumo concreto.
- **`grupo_ingrediente.RACIONES_SEMANA` admite `NULL`**, que representa «sin límite» (∞).
- **Restricciones añadidas:**
  - `CHECK` de `DIFICULTAD` con comparación binaria: solo valen exactamente `Fácil`, `Media` y `Difícil`.
  - `CHECK` de `TIEMPO > 0`, `COMENSALES > 0` y `RACIONES_SEMANA` positiva o `NULL`.
  - `UNIQUE(TOKEN_PASSWORD)`.
- **`PUBLICADA` se declara `BOOLEAN`:** en MySQL es exactamente `TINYINT(1)`, pero no genera el aviso de «display width» obsoleto.
- **Colación `utf8mb4_0900_ai_ci` en todas las tablas:**
  - Los `UNIQUE` de nombre de ingrediente, id de usuario y email no distinguen mayúsculas **ni acentos**: «Limon» choca con «Limón».
  - Los filtros por texto (6.5) ignoran mayúsculas y acentos sin código adicional.
- **Clasificación de ingredientes discutibles** (en `V2__seed.sql`; un admin puede cambiarlas):
  - patata → Hortalizas
  - harina, vinagre, levadura y maicena → Esenciales
  - jamón serrano, jamón cocido, bacon, chorizo y salchichas → Carne grasa
  - carrillada y lomo de cerdo → Carne
  - chocolate blanco, chocolate negro y galletas → Dulces
  - azúcar → Esenciales, como indica la especificación

## D-015 · Datos de demostración: migración repetible y callback de Flyway
- **Fase:** 1
- **Motivo:** los datos demo solo deben existir en `dev` (`db/dev`). Además, los consumos tienen que caer siempre en los últimos 10 días, algo que una migración versionada (se ejecuta una sola vez) no garantiza. Tampoco se pueden usar números de versión propios en `db/dev` sin romper el orden con futuras migraciones.
- **Decisión:**
  - `db/dev/R__datos_demo.sql`, migración **repetible** e idempotente (upsert): 3 usuarios (contraseña `demo1234`), 10 recetas originales con ID fijo del 1 al 10, sus ingredientes y los likes. Flyway solo la vuelve a aplicar si cambia su contenido, y la fecha de creación de las recetas se conserva.
  - `db/dev/afterMigrate__consumos_demo.sql`, **callback** que Flyway ejecuta tras cada `migrate`, es decir, en cada arranque. Borra los consumos de las 3 cuentas demo y los vuelve a crear con fechas relativas a `NOW()`.
- **Consecuencias:**
  - Los consumos que se registren con las cuentas demo se pierden al reiniciar en `dev`. Para pruebas manuales que deban persistir, hay que registrar otra cuenta.
  - El historial de `lucia` reproduce el ejemplo de la sección 6.4 y `marcos` tiene agotado el grupo Dulces.
  - Las imágenes de las recetas demo están en `src/main/resources/demo/imagenes/` y se generan con `scripts/generar-imagenes-demo.ps1`. Servirlas es trabajo de la fase 4.

## D-016 · Tests de integración con o sin Docker
- **Fase:** 1
- **Decisión:** `@TestIntegracion` arranca el contexto con el perfil `test` y `BaseDatosTestConfig`:
  - **Con Docker:** un `MySQLContainer` (`mysql:8.4`) de Testcontainers con `@ServiceConnection`.
  - **Sin Docker:** la base de datos de `TEST_DB_URL` (por defecto, `toca_test` en el MySQL portable), con las credenciales de `DB_USER` y `DB_PASSWORD`.
  - En ambos casos, un `FlywayMigrationStrategy` hace `clean` + `migrate` al crear el contexto.
- **Salvaguarda:** antes de cualquier `clean`, la base de datos debe llamarse `*_test`; si no, el test falla sin borrar nada.
- **Consecuencias:**
  - `mvnw verify` necesita Docker o el MySQL portable arrancado.
  - Sin Docker, Testcontainers registra en el log un `ERROR` («Could not find a valid Docker environment»). Es esperado.
  - El camino con Testcontainers no se ha podido ejecutar en esta máquina, porque no hay Docker.

## D-017 · MyBatis añadido a mano al `pom.xml`
- **Fase:** 1
- **Motivo:** Spring Initializr todavía rechaza la dependencia `mybatis` con Boot 4.1.2 («not compatible»). Sin embargo, `mybatis-spring-boot-starter` 4.1.0 está compilado contra Spring Boot 4.1.0.
- **Consecuencias:** la versión va fijada en la propiedad `mybatis-spring-boot.version` del `pom.xml`, porque Spring Boot no la gestiona.

## D-018 · jjwt con Gson para el JSON de los tokens
- **Fase:** 2
- **Motivo:** jjwt 0.13 solo trae módulo para Jackson 2 (`jjwt-jackson`), mientras que Spring Boot 4 usa Jackson 3. Jackson 2 ya está en el classpath porque lo arrastra springdoc (swagger-core), pero se prefiere no atar el JWT a una versión de Jackson que desaparecerá.
- **Decisión:** `jjwt-api` + `jjwt-impl` + `jjwt-gson` (Gson 2.13, gestionado por Boot). Boot no usa Gson para la API mientras Jackson esté presente.

## D-019 · Los roles viajan en el token
- **Fase:** 2
- **Decisión:** el filtro JWT reconstruye el usuario (id, nombre, roles) a partir del token, sin consultar la base de datos en cada petición.
- **Consecuencias:**
  - Un cambio de rol (fase 6) se aplica en el siguiente inicio de sesión del afectado. Como mucho tarda lo que dura el token (8 h por defecto).
  - Cambiar la contraseña no invalida los tokens ya emitidos.
- **Actualización (D-070):** un cambio de rol o de contraseña anula las sesiones abiertas.

## D-020 · Correos asíncronos tras el commit
- **Fase:** 2
- **Decisión:** el servicio publica un evento (`UsuarioRegistrado`, `RecuperacionPasswordSolicitada`). `CorreoListener` lo procesa con `@Async` + `@TransactionalEventListener(AFTER_COMMIT)`.
- **Consecuencias:**
  - Solo se envía si la transacción se confirma.
  - La respuesta HTTP no espera al servidor de correo.
  - Si el SMTP falla, el registro sigue siendo válido y el error queda en el log.
- **Ajustes de JavaMail:**
  - Timeouts de 5 s, para que un SMTP que no responde no bloquee hilos.
  - `mail.from` en la sesión.
  - En `dev`, `mail.smtp.localhost`.
  
  Sin los dos últimos, JavaMail resuelve el nombre del equipo para el `EHLO` y el `Message-ID`, y en esta máquina eso tardaba unos 5 s por correo.

## D-021 · «He olvidado mi contraseña» no revela si el email existe
- **Fase:** 2 · **Desviación del caso de uso de la memoria**, que mostraba «el correo no existe».
- **Decisión:** `POST /api/auth/password/olvido` responde siempre 200 con el mismo mensaje.
- **Detalles del enlace:**
  - Si el email existe, se genera un token aleatorio de 256 bits (Base64URL) que solo viaja en el correo.
  - En `TOKEN_PASSWORD` se guarda su SHA-256 y en `TOKEN_EXPIRACION`, ahora + 1 h.
  - Al restablecer, o al cambiar la contraseña desde el perfil, el token se anula: es de un solo uso.

## D-022 · Rate limiting en memoria por IP
- **Fase:** 2
- **Decisión:** `FiltroLimiteIntentos` cuenta todas las peticiones, también las mal formadas, con una ventana fija por IP:
  - `POST /api/auth/login`: 10 por minuto;
  - `POST /api/auth/password/olvido`: 5 cada 15 minutos.
  
  Al superarlo, responde 429 con `Retry-After`. Los límites se configuran en `app.limite-intentos`.
- **Consecuencias:**
  - ~~Es memoria de una sola instancia: con varias réplicas haría falta un almacén compartido (Redis, Bucket4j…).~~ Sustituido por D-071: las ventanas están en la base de datos.
  - Detrás de un proxy, la IP sale de `X-Forwarded-For` gracias a `server.forward-headers-strategy=framework` (perfil `prod`). En `dev` esa cabecera se ignora.

## D-023 · Email del administrador inicial configurable
- **Fase:** 2
- **Motivo:** `usuarios.EMAIL` es obligatorio y único, pero la especificación no da un email para `admin`.
- **Decisión:** variable opcional `ADMIN_INITIAL_EMAIL` (por defecto `admin@toca.local`).
- **Cuándo no se crea `admin`:**
  - si falta `ADMIN_INITIAL_PASSWORD` o tiene menos de 8 caracteres;
  - si el email ya lo usa otra cuenta.
  
  En ambos casos se registra un `WARN`. Si `admin` ya existe, no se toca su contraseña.

## D-024 · Contraseñas de 8 a 72 caracteres
- **Fase:** 2
- **Motivo:** la especificación fija un mínimo de 8. BCrypt solo usa los primeros 72 bytes y Spring Security rechaza contraseñas más largas.
- **Decisión:** `@Size(min = 8, max = 72)` en el registro, el cambio y el restablecimiento de contraseña. **Cambiado en D-090:** y como mucho 72 bytes (`@CabeEnBcrypt`), porque `@Size` cuenta caracteres: 40 «ñ» pasaban y daban 500.

## D-025 · Logs de SQL desactivados en `dev`
- **Fase:** 2
- **Motivo:** con `es.alimentarse` en DEBUG, MyBatis escribe en el log los parámetros de cada SQL, con los hashes de contraseña y de token incluidos.
- **Decisión:** `es.alimentarse` en INFO también en `dev`. Para depurar las SQL de un mapper concreto se activa solo ese logger, por ejemplo `es.alimentarse.usuario.UsuarioMapper: DEBUG`.

## D-026 · Paginación: página desde 0 y como máximo 100 elementos
- **Fase:** 3
- **Motivo:** la especificación fija los parámetros `page` y `size` y el formato de respuesta, pero no la base de la numeración ni los límites.
- **Decisión:**
  - `page` empieza en 0, igual que en Spring Data. La interfaz mostrará `page + 1`.
  - `size` va de 1 a 100, validado con `@Min`/`@Max` en el `@RequestParam` (400 `VALIDACION` con `errores.size`).
  - Una página más allá del final devuelve `contenido: []` con los totales correctos, sin error.
  - El record común `es.alimentarse.comun.Pagina` calcula `totalPaginas`.

## D-027 · Búsquedas de texto: «contiene» sin mayúsculas ni acentos y sin comodines
- **Fase:** 3
- **Decisión:**
  - Los filtros de texto usan `LIKE` con la colación `utf8mb4_0900_ai_ci`, que ya ignora mayúsculas y acentos.
  - El texto del usuario se escapa con `Texto.patronContiene`/`patronEmpiezaPor` (`\` → `\\`, `%` → `\%`, `_` → `\_`) y la consulta lleva `ESCAPE '\\'`. Así `%` y `_` se buscan literalmente.

## D-028 · Sugerencias de ingredientes
- **Fase:** 3
- **Decisión:**
  - `GET /api/ingredientes/sugerencias?q=` devuelve como mucho 10 ingredientes que contienen `q`.
  - Primero van los que empiezan por `q` y después el resto, cada bloque por nombre.
  - Con `q` vacío devuelve `[]` sin consultar la base de datos.

## D-029 · Los grupos de alimentos no se editan desde la API
- **Fase:** 3
- **Motivo:** la especificación dice que las raciones semanales son «valores iniciales, editables», pero la API de la sección 7 solo prevé `GET /grupos`.
- **Decisión:** los grupos y sus raciones se cambian con una migración Flyway nueva. Si hiciera falta editarlos desde la interfaz, se añadiría un `PUT /api/admin/grupos/{id}`.
- **Sustituida por D-065:** desde el bloque 2 de mejoras, los grupos se editan desde la administración.

## D-030 · Ids generados con `LAST_INSERT_ID()`
- **Fase:** 3
- **Motivo:** los modelos de MyBatis son records, inmutables, y `useGeneratedKeys` necesita un objeto con setter donde escribir el id.
- **Decisión:** tras el `INSERT`, el mapper ejecuta `SELECT LAST_INSERT_ID()` (`ultimoIdInsertado()`) dentro de la misma transacción `@Transactional`. Al usar una sola conexión, el valor es el del `INSERT` propio.

## D-031 · Orden del listado de recetas
- **Fase:** 4
- **Motivo:** la especificación fija el orden de la recomendación (likes y fecha), pero no el del listado general.
- **Decisión:**
  - `/api/recetas` y `/api/recetas/mias`: de la más reciente a la más antigua (`FECHA_CREACION DESC, ID DESC`).
  - Mejor valoradas: likes de más a menos, luego fecha.
  - El orden lo elige el enum `OrdenRecetas` mediante un `<choose>` en el XML; no se concatena SQL.

## D-032 · Visibilidad y permisos de las recetas
- **Fase:** 4
- **Decisión:**
  - Una receta no publicada «no existe» para nadie más que su autor: el detalle, la edición, la publicación y los likes responden 404. Así no se revela que existe.
  - Si la receta es visible (publicada) pero de otro usuario, editarla o publicarla responde 403.
  - La comparación del autor no distingue mayúsculas, igual que los ids en la base de datos.

## D-033 · «Me gusta» idempotentes y solo en recetas publicadas
- **Fase:** 4
- **Decisión:**
  - `POST` y `DELETE /api/recetas/{id}/like` son idempotentes (`INSERT … ON DUPLICATE KEY UPDATE`) y devuelven el total actualizado.
  - Dar «me gusta» a una receta no publicada (solo puede intentarlo su autor) responde 400 `RECETA_NO_PUBLICADA`.
  - Quitarlo se permite siempre que la receta sea visible.
  - Un usuario puede dar «me gusta» a sus propias recetas: la especificación no lo prohíbe.

## D-034 · Imágenes de las recetas
- **Fase:** 4
- **Tipo real:** se comprueba por los bytes iniciales (JPEG `FF D8 FF`, PNG `89 50 4E 47 0D 0A 1A 0A`, WebP `RIFF….WEBP`) con una comprobación propia, sin Apache Tika. La extensión del fichero y el `Content-Type` del cliente se ignoran.
- **Nombre y ubicación:** `UUID` + extensión del tipo detectado, en `UPLOAD_DIR`. Desde D-069, todas las imágenes nuevas son `.jpg`.
- **Limpieza ligada a la transacción:**
  - si la transacción que guarda la receta se deshace, la imagen nueva se borra;
  - al sustituir una imagen, la anterior se borra solo tras el commit.
- **Respaldo para las demo:** `GET /api/imagenes/{nombre}` busca primero en `UPLOAD_DIR` y después en `classpath:demo/imagenes/`. Así las recetas demo tienen imagen sin copiar ficheros.
- **Validación del nombre:** se exige `[A-Za-z0-9_-]+\.(jpg|png|webp)`, lo que impide el path traversal.
- **Límite de tamaño:** el límite de multipart (5 MB por fichero) responde 413 `IMAGEN_DEMASIADO_GRANDE` con `errores.imagen`. Para que el 413 llegue al cliente en vez de un corte de conexión, Tomcat descarta hasta 10 MB (`server.tomcat.max-swallow-size`).

## D-035 · Saneado del HTML de las instrucciones
- **Fase:** 4
- **Decisión:** OWASP Java HTML Sanitizer con las políticas predefinidas `FORMATTING` + `BLOCKS` + `LINKS`:

  | Se permite | Se quita |
  |---|---|
  | Párrafos, `div`, títulos `h1`–`h6`, listas y citas | Scripts |
  | Negrita, cursiva, subrayado, tachado, `sub`/`sup`, `br` | Atributos de eventos |
  | Enlaces `http`/`https`/`mailto`, con `rel="nofollow"` | Estilos e imágenes |

- **Consecuencias:**
  - Se sanea al guardar, así que lo que devuelve la API ya es seguro. Angular volverá a sanearlo al mostrarlo.
  - Si tras el saneado no queda texto visible, las instrucciones se rechazan.

## D-036 · Alta y edición de recetas en multipart
- **Fase:** 4
- **Decisión:**
  - La petición lleva la parte `receta` (JSON, con `Content-Type: application/json`) y la parte `imagen`, opcional.
  - Al editar sin imagen se conserva la actual.
  - Quitar la imagen de una receta sin sustituirla queda fuera del alcance: la especificación no lo pide.

## D-037 · Límites de validación de las recetas
- **Fase:** 4
- **Decisión:**

  | Campo | Límite |
  |---|---|
  | `tiempo` | De 1 a 1440 minutos |
  | `comensales` | De 1 a 100 |
  | `instrucciones` | Como mucho 20 000 caracteres de entrada y 60 000 tras sanear (el tipo `TEXT` admite 65 535 bytes) |
  | `ingredientes` | Al menos uno, sin repetidos y todos existentes; sin ingredientes no se podría calcular el máximo semanal (6.1) |
  | `cantidad` | Obligatoria, como mucho 100 caracteres |
  | `dificultad` | Exactamente `Fácil`, `Media` o `Difícil` |

- **Mensajes de error:**
  - Los parámetros que no se pueden convertir (`tipo=abc`) muestran «Valor no válido» en lugar del mensaje técnico de Spring.
  - Los errores de los filtros (`@ModelAttribute`) se devuelven con el nombre de cada campo.

## D-038 · Ventana de los «últimos 7 días»
- **Fase:** 5
- **Decisión:**
  - La ventana es móvil y va de `ahora − 7 días` a `ahora`, **ambos extremos incluidos**. Son instantes en UTC; `ahora` sale del `Clock` de la aplicación.
  - Un consumo con fecha posterior a `ahora` no cuenta.
  - Cada consumo cuenta una ración en cada grupo que aparece en su receta, aunque tenga varios ingredientes de ese grupo (6.2).
- **Implementación:** las reglas son funciones puras de `RecomendacionService` (`racionesPorGrupo`, `gruposAgotados`, `indicadores`), con tests exhaustivos de los bordes. La consulta SQL aplica la misma ventana con `BETWEEN`.

## D-039 · Reglas de los consumos
- **Fase:** 5
- **Decisión:**
  - `fecha` es opcional al registrar; si falta, se usa «ahora».
  - Se guarda sin fracciones de segundo, porque la columna es `TIMESTAMP`.
  - No se admiten fechas futuras. Para tolerar el desfase de reloj del cliente hay un margen de 60 s: una fecha futura dentro del margen se guarda como «ahora».
  - Solo se pueden registrar recetas visibles para el usuario: publicadas o propias.
  - Cada usuario solo ve, edita y borra sus consumos. Los ajenos responden 404, no 403, para no revelar que existen.
  - Si el autor despublica después una receta, el historial de quien ya la consumió la conserva.
  - El histórico es paginado (9 por página), del más reciente al más antiguo.

## D-040 · Resumen de «Mi alimentación»
- **Fase:** 5
- **Decisión:**
  - Recetas más consumidas (3) e ingredientes más usados (4): sobre **todo el historial**, porque la especificación no los limita a los 7 días.
    - Las recetas empatadas se ordenan por el consumo más reciente.
    - Los ingredientes empatados, por nombre.
    - ~~Se cuentan todos los ingredientes, también los básicos como la sal o el aceite.~~ Sustituido por D-054: los grupos básicos no se cuentan.
  - Indicadores (6.4):
    - `porcentaje` = `round(consumidas/límite·100)`, con un tope de 100; los grupos sin límite, 100.
    - `color` es un enum (`VERDE`, `AMARILLO`, `ROJO`); la interfaz elige el tono.
    - Orden: los grupos con límite de más a menos lleno y después los ilimitados por nombre, para que lo crítico salga primero.
- **Nota técnica:** las consultas de «más consumidas» e «ingredientes más usados» devuelven filas planas. Un resultMap de MyBatis con un objeto anidado y sin `idArg` en el objeto exterior **fusiona** las filas que coinciden en sus columnas simples (aquí, el mismo número de «veces»). Las tarjetas de receta se cargan después por id.

## D-041 · Recomendación «¿Qué toca hoy?»
- **Fase:** 5
- **Decisión:**
  - `GET /api/recetas/recomendadas` es paginado (9 por página) y admite los mismos filtros que el listado (6.5).
  - Se aplican a la vez el filtro de publicadas y la exclusión de grupos agotados: ningún ingrediente de esos grupos.
  - Orden: likes de más a menos y después fecha.
  - Las recetas propias publicadas también se recomiendan.
  - El controlador está en el paquete `alimentacion`, para que `receta` no dependa de él; la ruta literal tiene prioridad sobre `/api/recetas/{id}`.

## D-042 · Columnas de la tarjeta de receta reutilizables con un prefijo
- **Fase:** 5
- **Motivo:** las consultas de consumos devuelven la tarjeta completa de la receta con el resultMap `resumen` de `RecetaMapper`, que necesita las columnas con un prefijo (`columnPrefix="R_"`).
- **Decisión:** `<sql id="columnasResumen">` usa `${prefijo}` para los alias. Es una **propiedad de `<include>`** que MyBatis sustituye al cargar el XML, siempre con una constante (`""` o `R_`); nunca lleva datos de la petición.
- **Consecuencia:** es la única excepción a la norma de no usar `${…}` en los mappers.

## D-043 · Administración: rol comprobado en la base de datos y cambios serializados
- **Fase:** 6
- **Motivo:** con D-019, un administrador al que se le quita el rol conserva ADMIN en su token hasta que caduca (8 h). Además, si dos administradores se quitaban el rol el uno al otro a la vez, ambos podían ver «hay dos» y el sistema quedaría sin ninguno.
- **Decisión:**
  - Los endpoints de `/api/admin/**` exigen el rol en el token (`SecurityConfig`) **y además** consultan en la base de datos que el solicitante siga siendo administrador. Si no, responden 403. El resto de la API mantiene D-019.
  - Cada cambio de rol empieza con `SELECT … FROM roles WHERE NOMBRE = 'ADMIN' FOR UPDATE`, que bloquea la fila del rol hasta el final de la transacción. Así los cambios se aplican de uno en uno y las comprobaciones posteriores ven el estado confirmado.
  - Después del bloqueo se comprueba, en este orden:
    1. el solicitante sigue siendo administrador (403);
    2. el usuario existe (404);
    3. no se quita el rol a sí mismo (400 `PROPIO_ROL`);
    4. queda al menos otro administrador (409 `ULTIMO_ADMINISTRADOR`).
  - Es idempotente: dar el rol a quien ya lo tiene, o quitárselo a quien no lo tiene, no cambia nada.

## D-044 · Búsqueda de usuarios en la administración
- **Fase:** 6
- **Decisión:**
  - `q` busca «contiene» en el id, el nombre, **los apellidos** y el email. La especificación dice «id, nombre o email»; los apellidos se añaden porque la tabla muestra el nombre completo.
  - Ordenada por id.
  - 10 por página por defecto (la especificación no lo fija).

## D-045 · Qué pantallas son públicas
- **Fase:** 7
- **Motivo:** toda la API, salvo `/api/auth/**` y las imágenes, exige sesión, así que no hay contenido que enseñar a un visitante. En cambio, la información legal debe poder leerse **antes** de registrarse.
- **Decisión:**
  - Sin sesión solo se ven el login, el registro, la recuperación de contraseña y las páginas del pie (Ayuda, Información legal, Sobre nosotros), además de la página 404.
  - El resto de rutas, incluida la portada, pasa por `authGuard`, que lleva al login con `returnUrl` (salvo desde `/`).
  - Sin sesión, la barra de navegación solo muestra «Iniciar sesión» y «Registrarse».
  - `/restablecer-password` no lleva `invitadoGuard`: el enlace del correo debe funcionar aunque haya una sesión abierta en ese navegador.

## D-046 · Sesión del frontend en localStorage
- **Fase:** 7
- **Decisión:**
  - El JWT y los datos básicos del usuario (`{token, usuario}` del login) se guardan en `localStorage` con la clave `alimentarse.sesion`, para que la sesión sobreviva a recargas y a nuevas pestañas durante las 8 h del token.
  - Al arrancar y en cada guard se lee el `exp` del token: si ha caducado, la sesión se descarta sin esperar al 401.
  - Cerrar sesión solo borra el almacenamiento: con JWT sin estado no hay nada que invalidar en el servidor (D-019).
- **Sustituida por D-070:** el JWT ya no está en `localStorage`, sino en una cookie `HttpOnly`.
- **Riesgo asumido:** un XSS podría leer el token. Se mitiga con el saneado del HTML de las recetas (D-035) y con el escapado y el saneado que Angular aplica a las plantillas y a `[innerHTML]`.

## D-047 · Tratamiento de errores en el frontend
- **Fase:** 7
- **Decisión:**
  - **Interceptor (`authInterceptor`):**
    - añade `Authorization: Bearer …` a `/api/**` salvo a `/api/auth/**`;
    - un **401** fuera de `/api/auth/**` cierra la sesión, avisa y lleva a `/login?returnUrl=<página actual>`;
    - los fallos de red (status 0) y los **5xx** se avisan con un toast;
    - los **4xx** los trata cada pantalla.
  - **Errores por campo:** los `errores` de la API (por ejemplo, el 409 del registro) se colocan junto a cada control con la clave `servidor` (`aplicarErroresServidor`). Desaparecen en cuanto el usuario cambia el valor. Si no hay errores por campo, se muestra el `mensaje` en una alerta.
  - **Errores no capturados:** `ManejadorErroresGlobal` (un `ErrorHandler`) los registra en consola y muestra un aviso genérico.
  - **`returnUrl`:** solo se aceptan rutas internas (`destinoSeguro`): empiezan por `/`, no por `//` ni `/\`. Así un enlace manipulado no puede redirigir a otro sitio.

## D-048 · Angular 22: OnPush por defecto, formularios reactivos y textos de ng-bootstrap
- **Fase:** 7
- **Motivo:** en Angular 22 los componentes son `OnPush` por defecto y la regla de ESLint `prefer-on-push-component-change-detection` impide volver a `Eager`. El estado de un `FormControl` no es una signal, así que un componente hijo `OnPush` no se enteraría de que el control ha pasado a ser inválido o se ha tocado.
- **Decisión:**
  - `ErrorCampo` se suscribe a `control.events` (valor, estado, tocado…) e incrementa una signal para recalcular el mensaje. Los demás componentes leen el formulario en su propia plantilla, que ya se refresca con los eventos del DOM, y usan signals para el estado asíncrono (`enviando`, `error`).
  - Se mantienen los formularios reactivos que pide la especificación, no los *Signal Forms*.
  - `LOCALE_ID` = `es` (fechas y números en español). Las etiquetas accesibles de ng-bootstrap («Close», «Next month»…) se traducen al español con `loadTranslations` en `main.ts` (`src/locale/ngb-es.ts`). Es la única lengua de la interfaz, así que no hace falta un build por idioma.

## D-049 · Presupuesto del bundle inicial
- **Fase:** 7
- **Motivo:** el presupuesto por defecto de Angular (aviso a 500 kB) se supera solo con Bootstrap compilado, animate.css, ng-bootstrap y Font Awesome: unos 754 kB sin comprimir, **~150 kB transferidos**.
- **Decisión:** el aviso salta a 900 kB y el error a 1,2 MB. Las pantallas van en *lazy loading*, de modo que el bundle inicial apenas crece en las fases siguientes.
- **Resultado final:** ~800 kB sin comprimir y ~164 kB transferidos, dentro del presupuesto. El editor TipTap (~112 kB comprimidos) va aparte, en el *chunk* del formulario de receta.

## D-050 · Editor de las instrucciones: TipTap
- **Fase:** 8
- **Motivo:** la especificación pide un «editor enriquecido» y el backend sanea el HTML con una lista blanca (D-035). Las envolturas de Quill para Angular van por detrás de cada versión de Angular.
- **Decisión:**
  - TipTap 3 (`@tiptap/core` + `@tiptap/starter-kit`), que no depende de Angular, envuelto en `EditorHtml`, un `ControlValueAccessor`.
  - Versión **fijada exacta** (3.32.0 al elegirla), publicada unas semanas antes de hoy, en lugar de la del mismo día. Desde D-072 la sube Dependabot.
  - Solo ofrece lo que conserva el saneado: negrita, cursiva, subrayado, subtítulo (`h3`), listas, cita y enlace. Se desactivan el código, el tachado y la línea horizontal.
  - El valor es `''` si no hay contenido, y se quitan los párrafos vacíos del final. El validador `htmlConTexto` exige texto visible, igual que el backend tras sanear.
  - Va en el *chunk* del formulario (~112 kB comprimidos), que solo se descarga al crear o editar una receta.
  - Accesibilidad: el área editable tiene `role="textbox"`, `aria-multiline` y `aria-labelledby`; la barra es un `role="toolbar"` con `aria-pressed` en cada formato.

## D-051 · Estado de los listados en memoria, no en la URL
- **Fase:** 8
- **Motivo:** al volver del detalle de una receta, el listado debe seguir como estaba. Llevar los filtros a la URL obligaría a recuperar los nombres de los ingredientes de los chips a partir de sus ids, y la API no tiene esa consulta.
- **Decisión:**
  - `EstadoListados` guarda en memoria el filtro y la página de cada listado (recetas, mis recetas, ingredientes) mientras dura la pestaña. Se vacía al iniciar o cerrar sesión.
  - Única excepción: `?titulo=` en `/recetas`. Lo usa el buscador de la portada cuando se pulsa Intro sin elegir una sugerencia.
  - Si la página guardada ya no existe (por ejemplo, porque ahora hay menos resultados), se carga la última.

## D-052 · «Añadir a consumidos» desde el detalle (fase 8)
- **Fase:** 8
- **Decisión:**
  - El detalle de receta es de la fase 8, así que su «Añadir a consumidos» (6.7) se hace ya. El resto de los consumos (histórico y edición) llega en la fase 9.
  - El selector es un `datetime-local` con «ahora» por defecto, en hora del navegador, y la fecha se convierte a UTC antes de enviarla.
  - No admite fechas futuras, con 1 minuto de margen, igual que el backend. El máximo del selector se actualiza al enfocarlo, porque «ahora» avanza mientras la página está abierta.

## D-053 · Detalles de la interfaz de recetas e ingredientes
- **Fase:** 8
- **Decisión:**
  - **Dificultad:** termómetro verde, ámbar o rojo con el texto al lado, para que el color no sea la única pista. El ámbar es oscuro (`#a35f00`) para tener contraste sobre blanco.
  - **«Me gusta»:** el corazón está deshabilitado en las recetas no publicadas, que no lo admiten (D-033).
  - **«Mis recetas»:** cada tarjeta indica «Publicada» o «No publicada».
  - **Formulario de receta:**
    - pide confirmación al salir con cambios sin guardar (`canDeactivate`);
    - la imagen se valida en el navegador (tipo y 5 MB) antes de subirla;
    - los errores del servidor sobre `ingredientes[n]` e `imagen` se muestran junto a esas secciones.
  - **Portada:**
    - las sugerencias aparecen a partir de 2 caracteres;
    - elegir una abre la receta, y pulsar Intro lleva al listado filtrado por título;
    - los ingredientes, en cambio, se sugieren desde el primer carácter.
  - **Detalle en móvil:** los datos (ingredientes, grupos, recomendación, consumo) van antes de las instrucciones. En escritorio ocupan una columna lateral.
  - **Ilustración de la portada:** un SVG propio (`public/img/portada.svg`).

## D-054 · «Ingredientes más usados» sin los grupos básicos
- **Fase:** 9 (cambio en el backend, a petición del usuario)
- **Motivo:** la sal, el aceite, el agua o las especias están en casi todas las recetas, así que llenaban siempre la lista de «Ingredientes más usados» (D-040) y no decían nada de la alimentación de cada uno.
- **Decisión:**
  - La migración `V3__grupos_basicos.sql` añade la columna `BASICO` a `grupo_ingrediente` (por defecto `FALSE`) y la marca en **Esenciales** y **Especias**. Es una columna más de las que define la sección 5.
  - La consulta de `ingredientesMasUsados` ignora los grupos básicos. La lista puede tener menos de 4 elementos.
  - La marca va en los datos y no en el código: para cambiar qué grupos son básicos basta con otra migración.
- **No cambia:** las raciones, los indicadores de los 7 días ni la recomendación. Los grupos básicos no tienen límite semanal, así que nunca influían en ellos.

## D-055 · Pantallas de alimentación
- **Fase:** 9
- **Decisión:**
  - **«Qué toca hoy»:**
    - el aviso informativo dice además qué grupos ha completado el usuario esta semana (los indicadores en rojo del resumen), para que se entienda por qué faltan algunas recetas. Si esa petición falla, la pantalla funciona igual;
    - si no queda ninguna receta, un mensaje distinto según haya filtros o no.
  - **«Mi alimentación»:**
    - las secciones van en el orden de la especificación: últimas, más consumidas, ingredientes más usados e indicadores de los 7 días;
    - en los indicadores (6.4), el estado también se da en texto (leyenda y texto oculto para lectores de pantalla), no solo con el color;
    - el ámbar del círculo es `#c77800`, con un contraste mayor de 3:1 sobre blanco (gráficos, WCAG 1.4.11).
  - **Los indicadores incluyen los grupos básicos:** la 6.4 pide un círculo por cada grupo consumido, así que Esenciales y Especias siguen apareciendo, siempre en verde (sin límite). D-054 solo afecta a «Ingredientes más usados».
  - **Histórico:**
    - 9 consumos por página. El modal tiene Cancelar, Eliminar y Guardar; Eliminar pide confirmación dentro del propio modal antes de borrar;
    - tras cambiar o borrar un consumo se vuelve a pedir la página, porque el orden y el total pueden cambiar.

## D-056 · Perfil y gestión de usuarios en el frontend
- **Fase:** 10
- **Decisión:**
  - **«Mi perfil»:**
    - el nombre de usuario y el rol se muestran, pero no se pueden cambiar: la API no lo permite;
    - al guardar, el nombre de la cabecera se actualiza enseguida (`Sesion.actualizarNombre`). El token no cambia, porque el nombre solo se usa para mostrarlo;
    - «Guardar los datos» está deshabilitado mientras no haya cambios. Si se intenta salir con cambios sin guardar, se pide confirmación;
    - el cambio de contraseña pide la actual y la nueva dos veces. Si la actual no es correcta, el error aparece junto a ese campo.
  - **Gestión de usuarios:**
    - 10 por página y búsqueda «contiene» (D-044); el interruptor de administrador tiene `aria-pressed`;
    - el del propio administrador está deshabilitado (el backend respondería `PROPIO_ROL`), y su fila lleva la marca «Tú»;
    - los errores `ULTIMO_ADMINISTRADOR` y similares se avisan con un toast. Un 403 (ya no es administrador en la base de datos, D-043) muestra un aviso para cerrar sesión y volver a entrar;
    - una nota recuerda que el cambio de rol se aplica a esa persona en su próximo inicio de sesión (D-019);
    - en móvil, la tabla se sustituye por tarjetas con la misma información, para no estrechar las columnas.
  - Ya no queda ninguna pantalla provisional, así que se elimina `EnConstruccion`.

## D-057 · PWA: iconos, caché sin conexión y privacidad
- **Fase:** 11
- **Decisión:**
  - **Iconos propios:** `ng add @angular/pwa` genera iconos y un `favicon.ico` con el logotipo de Angular, que no es nuestro.
    - Se sustituyen por dos SVG originales (tenedor y cuchillo sobre el marrón de la marca): `icono.svg` (*any*) e `icono-maskable.svg` (fondo a sangre y dibujo dentro de la zona segura del 80 %).
    - Los PNG los genera `scripts/generar-iconos-pwa.ps1`: Edge dibuja a 512 px y System.Drawing reduce, porque la ventana de Edge headless no baja de unos cientos de píxeles.
    - iOS usa `apple-touch-icon.png`, sin transparencia.
  - **Caché de la API (`dataGroups`, *freshness*, 3 s):**
    - se cachean las lecturas de recetas (listados, detalle, recomendadas, sugerencias), ingredientes, catálogos, resumen de «Mi alimentación» y también el histórico de consumos (`/api/consumos`), que forma parte de «Mi alimentación»;
    - las imágenes van en un grupo aparte (30 días, 200 entradas);
    - no se cachean el perfil, la administración ni la autenticación;
    - cada combinación de filtros y página es una entrada distinta: la URL incluye la consulta.
  - **Navegación:** las URL de `/api/**` no se responden con `index.html` (`navigationUrls`).
  - **Privacidad:** la caché del service worker no distingue usuarios ni mira la cabecera `Authorization`. Al iniciar y al cerrar sesión se vacían las cachés de datos de la API (`borrarDatosGuardados`), para que en un equipo compartido nadie vea sin conexión los datos de otra persona. Se borran las entradas y no las cachés, porque el service worker mantiene abiertas sus referencias.
  - **Sin conexión (`navigator.onLine` falso):**
    - el banner es sticky bajo la cabecera, con `role="status"`;
    - los botones que escriben usan la directiva `RequiereConexion`, que los deshabilita y los enlaza con el banner (`aria-describedby`);
    - al recuperar la conexión, un toast lo avisa.
    - Si el servidor cae pero el navegador sigue «en línea», no hay banner: el service worker sirve los datos guardados al fallar la red.
  - **Versiones:**
    - el aviso de versión nueva tiene «Recargar» y «Más tarde»; si el service worker queda en estado irrecuperable, solo «Recargar»;
    - además de en cada navegación, se buscan versiones nuevas cada 6 horas.
  - **Tipo MIME del manifest:** Spring Boot no tiene `.webmanifest` en su tabla de tipos y lo servía como binario. `ServidorWebConfig` añade `application/manifest+json`.
  - **Versión de `@angular/service-worker`:** la misma que el resto de Angular (22.2.4). La 22.2.5 exige `@angular/core` 22.2.5 exacto.
    - Desde D-072 todos los paquetes de Angular suben juntos (22.2.5, con el CLI en 22.2.6), y Dependabot los agrupa en un único pull request.

## D-058 · Empaquetado, rutas de Angular y pruebas e2e
- **Fase:** 12
- **Decisión:**
  - **JAR único:**
    - el perfil Maven `frontend` (`frontend-maven-plugin` 2.0.2, Node 24.22.0) compila Angular antes de `process-resources`;
    - el JAR se llama siempre `target/alimentarse.jar` (`finalName`);
    - sin el perfil, el build normal no depende de Node: los tests del backend no lo necesitan;
    - el perfil vacía antes `target/classes/static`, porque Maven no borra los ficheros que ya no existen y el JAR acumulaba bundles de builds anteriores.
  - **Servir el frontend (`FrontendConfig`):**
    - sustituye a la configuración estática de Spring Boot (`spring.web.resources.add-mappings: false`);
    - lo que no es `/api` y no tiene extensión en su último segmento devuelve `index.html`; un fichero inexistente sigue dando 404;
    - los ficheros con hash (`*.js`, `*.css`, `media/`) llevan `max-age` de un año e `immutable`, menos los scripts del service worker, que no tienen hash;
    - el resto conserva el `no-store` de Spring Security.
    - Los tests usan un build de prueba (`src/test/resources/static-prueba`), porque el real no se versiona.
  - **Perfil `e2e`:**
    - vacía la base de datos y vuelve a migrar al arrancar (`E2eConfig`, solo con `clean-disabled: false`, que pone `application-e2e.yml`) y sube el límite de intentos de login;
    - Playwright lo arranca contra `toca_test`, la base desechable de los tests del backend. No hay datos de demostración: cada prueba crea los suyos.
  - **Pruebas e2e en `frontend/e2e`:**
    - comparten `node_modules` con el frontend;
    - usan la extensión `*.e2e.ts` para que Vitest no las ejecute;
    - en Windows usan el Edge instalado (`channel: msedge`), sin descargar navegadores.
    - La revisión de responsive es automática: no puede haber scroll horizontal en ninguna pantalla a 375, 768 ni 1280 px. Las capturas van al informe.
  - **Docker:**
    - `Dockerfile` en tres etapas, con el usuario `toca` sin privilegios y `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75`;
    - el perfil `full` de Compose espera a que MySQL esté sano.
    - **No se ha podido construir ni probar en esta máquina, que no tiene Docker**; se ha revisado a mano.
  - **Avisos de npm:** npm 11 avisa de que no ejecuta los scripts de instalación de `esbuild`, `lmdb`, `msgpackr-extract` y `@parcel/watcher`. Ninguno hace falta: sus binarios llegan como dependencias opcionales y el build funciona. Con el perfil `frontend`, Maven muestra esos avisos como `[ERROR]`, pero no son errores.

## D-059 · Lighthouse y revisión final
- **Fase:** 13
- **Medición:** Lighthouse 13.5 en Edge, sobre el JAR (build de producción) con los datos de demostración. 11 pantallas, en escritorio y en móvil, con sesión de administrador.

  | Categoría | Escritorio | Móvil |
  |---|---|---|
  | Accesibilidad | 100 en todas | 100 en todas |
  | Buenas prácticas | 96–100 | 96–100 |
  | SEO | 100 | 100 |
  | Rendimiento (no es criterio de aceptación) | 81–100 | 77–100 |

  - **Instalable como PWA:** Lighthouse 12+ ya no tiene la categoría PWA, así que se comprueba con el propio navegador: `Page.getInstallabilityErrors` devuelve una lista vacía y el manifest no tiene errores.
- **Cambio tras la medición:** la paginación pasa del tamaño pequeño (`size="sm"`) al normal. Lighthouse marcaba los números de página como objetivos táctiles demasiado pequeños (WCAG 2.5.8) en «Ingredientes», que tiene varias páginas.
- **Buenas prácticas en 96, no en 100:** Lighthouse anota en la consola un 504 del service worker sobre el documento en las pantallas que este controla.
  - No se reproduce fuera de Lighthouse: ni navegando con Playwright o Puppeteer en el mismo Edge, ni con la caché desactivada, ni pasando por `about:blank` como hace Lighthouse, ni saltándose la auditoría de *back/forward cache*.
  - Se considera un artefacto de la instrumentación de Lighthouse con el service worker y se deja anotado.
- **Criterios de aceptación (12 de la especificación):** se cumplen todos menos uno, que no se ha podido comprobar: levantar la aplicación con `docker compose --profile full up --build`. En la máquina de desarrollo no hay Docker (D-058).
- **Guía de uso:** `docs/GUIA.md`, para usuarios y administradores (el equivalente al anexo 3 de la memoria, con texto propio). Sus capturas (`docs/img/guia/`) se tomaron con Playwright sobre los datos de demostración, sin guardar cambios.

## D-060 · Integración continua, cabeceras de seguridad, compresión y salud
- **Fase:** mejoras, bloque 1
- **Integración continua** (`.github/workflows/ci.yml`, GitHub Actions): en cada push a `main`, en cada pull request y a mano. Cuatro trabajos en paralelo:
  - **Backend:** `mvnw verify`, con un MySQL de Testcontainers porque GitHub tiene Docker. El informe de JaCoCo queda como artefacto.
  - **Frontend:** lint, formato, Vitest y build.
  - **e2e:** build de Angular, JAR y Playwright con Chromium contra un MySQL de servicio; secreto JWT de usar y tirar.
  - **Docker:** construye la imagen y levanta `docker compose --profile full --wait`. Comprueba portada, rutas de Angular, manifest, login del administrador, gzip, CSP y salud. **Con esto queda verificado el criterio de aceptación de Docker** (D-058, D-059).
  - La primera ejecución destapó un fallo que en local no se veía: sin un build real en `target/classes/static`, `/` daba 404. La página de bienvenida de Spring Boot solo existe si encuentra un `index.html`, y la ruta vacía no llega a los resolvers. Ahora `FrontendConfig` reenvía `/` a `index.html` siempre.
- **Cabeceras de seguridad** (`SecurityConfig`), en todas las respuestas:
  - **Content-Security-Policy:**
    - scripts solo del propio origen, sin `unsafe-inline` ni `unsafe-eval`;
    - estilos con `unsafe-inline`, porque Angular, ng-bootstrap y Font Awesome los insertan;
    - imágenes con `data:` (iconos SVG de Bootstrap) y `blob:` (vista previa de la imagen de una receta);
    - `frame-ancestors 'none'`, `object-src 'none'` y `base-uri`/`form-action 'self'`.
    - Comprobado en el navegador: ninguna violación en las pantallas, el formulario (editor, typeahead, modal, vista previa) ni Swagger UI.
  - **Otras:** `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy` sin cámara, micrófono, geolocalización, pagos ni USB, y `Cross-Origin-Opener-Policy: same-origin`. Se mantienen las de Spring Security: `X-Frame-Options: DENY`, `nosniff` y HSTS, que solo se envía por HTTPS.
  - **Build de Angular sin CSS crítico en línea** (`inlineCritical: false`): esa optimización mete un `<script>` en `index.html` que la CSP bloquearía y dejaría la página sin estilos. A cambio, la hoja de estilos se carga sin diferirla; afecta poco porque el service worker la guarda.
- **Compresión gzip** de Tomcat (`server.compression`) para JSON, HTML, CSS, JS, SVG y el manifest, a partir de 2 KB.
- **Salud:** Spring Boot Actuator solo con el endpoint `health`, bajo `/api` y público (`/api/health`, `/liveness`, `/readiness`), sin detalles.
  - El correo se excluye de la salud.
  - El resto de endpoints de Actuator no se exponen.
  - El `Dockerfile` tiene un `HEALTHCHECK` que pide `/api/health/readiness` con `bash` y `/dev/tcp`. La imagen de Temurin 25 no trae `curl` ni `wget`: la primera versión los usaba y la CI la marcó como no sana.
- **Seguir las ejecuciones** del repositorio, que es privado: con la API de GitHub y la credencial de Git del desarrollador, solo para leer el estado y los registros.

## D-061 · Eliminar una receta propia
- **Fase:** mejoras, bloque 2
- `DELETE /api/recetas/{id}`, solo el autor. La especificación solo preveía dejar de publicar.
- **Dos tipos de borrado:**
  - Si **nadie la ha consumido**, se borra del todo: la fila, sus ingredientes y sus «me gusta» (en cascada) y la imagen, tras el commit.
  - Si **alguien la ha consumido**, el autor incluido, se hace un **borrado lógico**: columna `recetas.ELIMINADA` (V4), sin publicar (`CK_RECETAS_ELIMINADA` impide que esté eliminada y publicada a la vez), sin imagen y sin «me gusta». Se conservan el título y los ingredientes.
  - Motivo: un borrado total haría desaparecer consumos de otros usuarios y les cambiaría las raciones de la semana. Así, la recomendación les seguiría ofreciendo un grupo que ya han agotado.
- **Una receta eliminada no existe para nadie**, tampoco para su autor:
  - `RecetaBase.visiblePara` la descarta: 404 en el detalle, la edición, la publicación, los «me gusta» y los consumos nuevos;
  - el filtro común de los listados (`<sql id="filtro">`) la excluye siempre, también de «Mis recetas».
  - Solo aparece en los consumos (histórico, últimas y más consumidas de «Mi alimentación»), con `eliminada: true`. La tarjeta la muestra sin enlace, sin foto y sin «me gusta», con la etiqueta «Eliminada».
- **Concurrencia:** `eliminar` empieza con `SELECT … FOR UPDATE` de la receta, antes de cualquier otra lectura.
  - En REPEATABLE READ, la instantánea de la transacción se toma en su primera lectura normal. Así, la comprobación «¿tiene consumos?» ve los consumos que otra transacción haya confirmado mientras se esperaba el bloqueo, y no borra del todo una receta recién consumida.
  - En el caso contrario, un consumo o un «me gusta» que choca con una receta recién borrada produce un error de clave ajena. `ManejadorErrores` traduce ahora cualquier `DataIntegrityViolationException` en un **409** `CONFLICTO` («Los datos han cambiado mientras tanto»), en lugar de un 500.
- **Frontend:** botón **Eliminar** en el detalle y papelera en las tarjetas de «Mis recetas». Las dos usan `ModalConfirmacion` (`compartido/modal-confirmacion.ts`, con `confirmar(modales, opciones)`), un modal reutilizable que ejecuta la acción dentro y muestra en él los errores 4xx.

## D-062 · «Recetas que me gustan»
- **Fase:** mejoras, bloque 2
- `GET /api/recetas/me-gustan`, con los filtros y la paginación del listado general, y la pantalla `/me-gustan` en el menú del usuario.
- **Orden:** por la fecha del «me gusta», del más reciente al más antiguo. Para eso, `like_receta` tiene ahora una columna `FECHA` (V5).
  - Los «me gusta» anteriores a la migración toman la fecha de la migración.
  - La fecha la pone el `Clock` de la aplicación; repetir un «me gusta», que es idempotente, no la cambia.
- **Solo las publicadas.** Si el autor deja de publicar una, desaparece de la lista sin perder el «me gusta», y vuelve si la publica otra vez. Una receta eliminada pierde sus «me gusta» (D-061).
- **Implementación:** un filtro más en `FiltroRecetas` (`meGustaDe`, un `EXISTS` sobre `like_receta`) y un orden más (`OrdenRecetas.ME_GUSTA_RECIENTES`). Así la consulta reutiliza el `<sql id="filtro">` común.
- Al quitar el corazón desde esta pantalla, la receta sale de la lista: se vuelve a pedir la página, porque cambia el total.

## D-063 · Moderación: recetas ocultas y usuarios bloqueados
- **Fase:** mejoras, bloque 2
- **Recetas ocultas** (V6: `recetas.OCULTA` y `MOTIVO_OCULTA`):
  - `PUT /api/admin/recetas/{id}/oculta` con un motivo obligatorio, que ve el autor, y `GET /api/admin/recetas/ocultas` para revisarlas.
  - Una receta oculta solo la ven su autor y los administradores (`RecetaBase.visiblePara(usuario, admin)`). Para los demás es un 404, como una no publicada.
  - Los administradores la ven por el rol del token (solo lectura); para ocultar o mostrar se comprueba el rol en la base de datos, como en el resto de la administración (D-043).
  - «Publicada» en los listados significa ahora «la ve todo el mundo»: `PUBLICADA = TRUE AND OCULTA = FALSE`. Así una receta oculta desaparece de la portada, el listado, «¿Qué toca hoy?», «Recetas que me gustan» y las sugerencias.
  - Ocultar es independiente de publicar. El autor puede editarla y dejar de publicarla, pero publicarla da **409 `RECETA_OCULTA`**. Al volver a mostrarla recupera su estado de publicación.
  - Solo se ocultan recetas que el administrador puede ver: publicadas o ya ocultas. Un borrador ajeno da 404.
  - No recibe «me gusta» nuevos; los que tenía se conservan.
  - Los consumos ya registrados no cambian y siguen contando en las raciones.
  - Solo su autor puede apuntarla como comida: el alta de consumos, el impacto (D-067) y el planificador usan la visibilidad sin el permiso de administrador. Por eso el detalle no ofrece «¿La has comido?» al administrador que la revisa (lo detectó el recorrido completo de la aplicación).
  - **No se envía correo** al autor: lo ve en «Mis recetas» (etiqueta «Oculta») y en el detalle, con el motivo. Si hiciera falta, se añadiría como los demás correos (D-020).
  - No hay un flujo de «pedir revisión»: el autor contacta con la administración, que revisa la lista de ocultas.
- **Usuarios bloqueados** (V6: `usuarios.BLOQUEADO`):
  - `PUT /api/admin/usuarios/{id}/bloqueado`.
  - No se puede bloquear uno mismo (400 `PROPIO_BLOQUEO`) ni a un administrador (409 `ADMIN_NO_BLOQUEABLE`: antes hay que quitarle el rol, para no dejar el sistema sin administradores por esta vía). Tampoco se puede hacer administrador a un bloqueado (409 `USUARIO_BLOQUEADO`).
  - Comparte el bloqueo `FOR UPDATE` de los cambios de rol (D-043): un bloqueo y un cambio de rol simultáneos no se cruzan.
  - **Login:** 403 `CUENTA_BLOQUEADA`, pero solo después de comprobar la contraseña. A quien no la sabe no se le revela que la cuenta existe y está bloqueada. Por eso no se usa `UserDetails.accountLocked`, que Spring comprueba antes que la contraseña.
  - **Sesiones abiertas:** el JWT dura 8 horas (D-019), así que `FiltroJwt` comprueba en **cada petición con token** que la cuenta existe y no está bloqueada (`EstadoCuentas`, una consulta por clave primaria).
    - Si no existe, la petición sigue sin autenticar y responde 401 `NO_AUTENTICADO`. Esto prepara la baja de cuentas (D-064).
    - Si está bloqueada, responde 401 `CUENTA_BLOQUEADA`, y el frontend cierra la sesión y muestra ese mensaje.
    - Los roles siguen saliendo del token (D-019); solo se comprueba el estado de la cuenta.
  - Consecuencia en los tests: el usuario de `tokens.usuario(id)` debe existir en la base de datos (`datos.usuario(id)`).
- **Frontend:**
  - en el detalle, **Ocultar (moderación)**, que abre `ModalOcultarReceta` con el motivo, y **Volver a mostrar**; el autor ve un aviso con el motivo;
  - **Administrador → Recetas ocultas**;
  - en la gestión de usuarios, una columna **Bloqueado** con su interruptor (deshabilitado, con el motivo en el `title`, para uno mismo y para los administradores) y la etiqueta «Bloqueado»;
  - la tarjeta muestra la etiqueta «Oculta» y no admite «me gusta».

## D-064 · Descargar mis datos y darse de baja
- **Fase:** mejoras, bloque 2
- Los derechos de acceso y de supresión del RGPD, sin pasar por la administración. Código en el paquete `cuenta`.
- **`GET /api/usuarios/me/datos`:** un JSON descargable (`Content-Disposition: attachment`, `no-store`) con:
  - el perfil;
  - las recetas propias no eliminadas, con instrucciones, ingredientes y la URL de la imagen;
  - los consumos;
  - los «me gusta».
  - No lleva la contraseña ni los tokens.
  - Las imágenes van como URL y no dentro del fichero, para no generar ficheros enormes.
  - El frontend la pide con la cabecera `ngsw-bypass`, para que el service worker no la guarde en su caché (D-057).
- **`POST /api/usuarios/me/baja`** con la contraseña. Es `POST` y no `DELETE` porque lleva cuerpo: hay proxies que descartan el cuerpo de un `DELETE`. En una sola transacción:
  1. Bloqueos antes de cualquier lectura: el de los cambios de rol (D-043) y `FOR UPDATE` de sus recetas (como en D-061).
  2. Se comprueban la contraseña (400 `PASSWORD_INCORRECTA`) y que no sea el último administrador (409 `ULTIMO_ADMINISTRADOR`).
  3. Se borran sus consumos, sus «me gusta» y los «me gusta» de sus recetas.
  4. Sus recetas que otros han consumido se **anonimizan**: eliminadas, sin autor, sin imagen y sin instrucciones. Solo conservan el título y los ingredientes, que hacen falta para el histórico y las raciones de esos usuarios.
     - V7 permite `recetas.USUARIO = NULL`, y `CK_RECETAS_AUTOR` lo limita a las eliminadas.
     - Una receta sin autor muestra la tarjeta sin la línea «por …».
  5. Se borran el resto de sus recetas, sus roles y el usuario.
  6. Tras el commit se borran las imágenes y se envía un correo de confirmación (`CuentaEliminada`, D-020).
- **Después de la baja:**
  - el token deja de valer al momento, por la comprobación de la cuenta en cada petición (D-063);
  - el nombre de usuario y el email quedan libres para una cuenta nueva, que no hereda nada: las recetas conservadas ya no tienen autor.
- **`AdminInicial`** ya no recrea `admin` si hay algún administrador. Antes, si `admin` se daba de baja habiendo otros administradores, volvía a aparecer en el siguiente arranque con la contraseña de `ADMIN_INITIAL_PASSWORD`.
- **Frontend:** la tarjeta «Tus datos y tu cuenta» en «Mi perfil», con «Descargar mis datos» y «Eliminar mi cuenta».
  - «Eliminar mi cuenta» abre `ModalBaja`, que explica qué se borra y pide la contraseña.
  - Tras la baja se cierra la sesión, se avisa y se vuelve al inicio de sesión.
  - «Información legal» explica los dos derechos.

## D-065 · Grupos de alimentos e ingredientes desde la administración
- **Fase:** mejoras, bloque 2
- **Sustituye a D-029.** La especificación habla de raciones «iniciales, editables», y una migración por cada cambio no era práctico.
- **Grupos** (`/api/admin/grupos`):
  - listado con las raciones, si es básico (D-054) y cuántos ingredientes tiene;
  - alta, edición y borrado. El nombre es único y las raciones van de 1 a 99 o son «sin límite».
  - Solo se borra un grupo sin ingredientes (409 `GRUPO_EN_USO`). Se bloquea su fila antes de contarlos, para que un ingrediente que se esté asignando a la vez espere.
  - **Efecto inmediato y retroactivo:** los indicadores (6.4) y la recomendación (6.3) se calculan al pedirlos, así que un cambio de raciones afecta enseguida a todos los usuarios, también sobre lo ya consumido en la ventana de 7 días. No hay histórico de raciones: guardar el valor vigente en cada consumo complicaría mucho la regla 6.2 para un caso poco frecuente.
  - El frontend guardaba los grupos durante toda la sesión. `CatalogoApi.invalidarGrupos()` hace que, tras un cambio, la siguiente pantalla los vuelva a pedir. En las sesiones de otros usuarios ya abiertas, los textos de raciones de las tarjetas de ingrediente se actualizan al recargar; los indicadores, que vienen del servidor, al momento.
- **Fusionar ingredientes** (`POST /api/admin/ingredientes/{id}/fusionar`): para los duplicados que crean los usuarios (6.8). En una transacción:
  1. `FOR UPDATE` de los dos ingredientes, siempre por orden de id, para no causar interbloqueos.
  2. En las recetas que llevan los dos, la cantidad del duplicado se suma en el texto de la del destino («2 + 1 unidad», cortado a 100 caracteres) y se quita la línea del duplicado.
  3. El resto de líneas pasan al destino, y el duplicado se borra.
  - Afecta también a las recetas eliminadas (D-061). Si los grupos son distintos, cambia el grupo con que cuentan esas recetas, también en los consumos registrados; el modal lo avisa antes de fusionar.
- **Borrar un ingrediente** (`DELETE /api/admin/ingredientes/{id}`): solo si ninguna receta lo usa (409 `INGREDIENTE_EN_USO`), para limpiar errores de escritura. Si lo usa alguna, se fusiona.
- Todo comprueba el rol en la base de datos (D-043). El código está en `CatalogoAdminService`, con `AdminService.exigirAdministrador`.
- **Frontend:**
  - **Administrador → Grupos de alimentos:** tabla en escritorio y tarjetas en el móvil, y `ModalGrupo`, con «Sin límite semanal» que desactiva las raciones;
  - en **Ingredientes**, para los administradores, los botones fusionar (`ModalFusionar`, con el typeahead de ingredientes) y borrar (`ModalConfirmacion`).

## D-066 · Marca visible: «Toca — ¿Qué toca hoy?»
- **Fase:** mejoras
- **Sustituye a la marca «QCH — ¿Qué como hoy?»** de la especificación y al nombre `qch` de D-007.
- **Motivo:** «toca» dice lo que hace la recomendación. No propone cualquier receta, sino la que corresponde según lo comido en la semana: «hoy toca pescado, que la carne ya está completa». Además es una palabra y no unas siglas, y cabe bajo el icono de la PWA.
- **Qué cambia:**
  - en la interfaz: el logotipo, el pie, el titular de la portada, la página sin JavaScript, el aviso de versión nueva, la ayuda, «Sobre nosotros» y la información legal;
  - la pantalla 6.3 pasa a llamarse **«¿Qué toca hoy?»**, en `/que-toca-hoy`. La ruta antigua, `/que-como-hoy`, redirige a la nueva para no romper los enlaces guardados;
  - el manifest de la PWA (`name` y `short_name`) y `apple-mobile-web-app-title`;
  - la cabecera de los correos y el título de la documentación OpenAPI;
  - **los nombres técnicos:** las bases de datos `toca` y `toca_test` (`toca_e2e` en la CI), el usuario de MySQL y el del contenedor (`toca`), el dominio `toca.local` de los correos de ejemplo y de los datos de demostración, y las variables de color `--toca-*`;
  - la guía y el README, con sus capturas.
- **Qué no cambia:** **Alimentarse** sigue siendo el nombre de la aplicación: el título de la pestaña («Página · Alimentarse»), el remitente y los asuntos de los correos, el repositorio y el paquete `es.alimentarse`.
- **Instalaciones que ya existían:**
  - La aplicación usa la base de datos y el usuario que digan `DB_URL` y `DB_USER`, así que un `.env` con los nombres antiguos sigue funcionando. Solo cambian los valores por defecto: sin `TEST_DB_URL` en el `.env`, los tests y las pruebas e2e buscan ahora `toca_test`.
  - Para pasar a los nombres nuevos en un MySQL local:
    1. crear `toca` y `toca_test` (`utf8mb4`, `utf8mb4_0900_ai_ci`) y el usuario `toca`, con todos los permisos sobre las dos;
    2. copiar los datos con `mysqldump qch | mysql toca` (incluye `flyway_schema_history`) y, cuando todo funcione, borrar `qch`. No conviene mover las tablas con `RENAME TABLE qch.tabla TO toca.tabla`: InnoDB sigue comprobando las claves ajenas contra `qch` hasta que se borran y se vuelven a crear;
    3. cambiar en el `.env` `DB_URL`, `DB_USER` y, si están, `TEST_DB_URL`, `MAIL_FROM` y `ADMIN_INITIAL_EMAIL`.
  - Con Docker, un volumen de MySQL ya creado conserva la base de datos `qch`: hay que migrarla igual o empezar de cero con `docker compose down -v`.
  - En `dev`, los emails de `lucia`, `marcos` y `carmen` pasan solos a `@toca.local` en el siguiente arranque, porque cambia la migración repetible. El de `admin` no: es un dato de la cuenta.
- **Administrador inicial:** el de las instalaciones nuevas se llama «Administrador Toca»; en las ya existentes no cambia (su perfil es editable).
- **Coincidencia conocida:** Toca Boca usa «Toca» en sus aplicaciones (por ejemplo, *Toca Kitchen*, un juego de cocina). Para un proyecto académico que no se publica en tiendas no es un problema; antes de publicarla habría que revisar la marca.

## D-067 · Recomendación: variedad, motivo y aviso antes de comer
- **Fase:** mejoras, bloque 3
- **Se aparta de 6.3** en el orden («nº de likes desc., luego fecha»). Con ese orden, lo más popular salía siempre primero, aunque ya se hubiera comido ese grupo varias veces en la semana. El conjunto de recetas recomendadas no cambia: la regla de los grupos agotados es la misma.
- **Orden por variedad**, en tres tramos, con «me gusta» y fecha solo dentro de cada tramo:
  1. ningún grupo con límite consumido esta semana;
  2. algún grupo con límite consumido, con su indicador en verde;
  3. algún grupo con el indicador en ámbar.
  - Son tramos y no una puntuación continua para que los «me gusta» sigan contando: con proporciones exactas casi nunca habría empates.
  - Los umbrales son los del indicador (6.4), para que lo que ve el usuario y el orden digan lo mismo.
  - Los grupos sin límite no penalizan: comer más fruta o verdura no es un problema de variedad.
  - Las reglas siguen en Java (`RecomendacionService`, funciones puras). A la consulta solo llegan los ids de los dos conjuntos, y el `ORDER BY` se resuelve en SQL porque la lista está paginada.
- **Por qué se recomienda:** cada receta llega con `grupos`, cómo van sus grupos esta semana, y el frontend redacta la frase («Sin consumir esta semana: Legumbres», «Te quedan raciones de Carne (2/3)»). Se omiten los grupos básicos sin límite (D-054), que no dicen nada.
- **JSON sin romper nada:** `RecetaRecomendadaDto` usa `@JsonUnwrapped`, así que la tarjeta sigue en el primer nivel y `grupos` es un campo más.
- **Aviso antes de comer** (`GET /api/recetas/{id}/impacto`):
  - Solo grupos con límite: en los demás no hay nada que avisar.
  - Se calcula para «ahora», aunque en el formulario se elija una fecha pasada. Es una orientación, no un cálculo exacto de esa fecha, y por eso el texto dice «Si la comes ahora».
  - Es una petición aparte del detalle de la receta para que `receta` no dependa de `alimentacion`, y si falla, la pantalla funciona igual.
  - Un administrador que mira una receta oculta de otro recibe 404 y no ve el aviso: tampoco podría registrarla como consumida.

## D-068 · Planificador semanal
- **Fase:** mejoras, bloque 3
- **Modelo:** tabla `usuario_receta_planificada` (V8), parecida a la de consumos, con una restricción única por usuario y momento: un hueco admite una sola receta.
- **Comida y cena:** cada día tiene dos huecos. La fecha se guarda como un instante, igual que los consumos, y el frontend la construye en hora local (14:00 y 21:00). Así el servidor no necesita conocer la zona horaria del usuario y la regla 6.2 se reutiliza tal cual.
- **Cómo cuenta (mismas funciones puras de `RecomendacionService`):**
  - Las comidas planificadas se suman a los consumos reales como filas más, con el id en negativo para que nunca coincidan con el de un consumo.
  - **Por comida:** se calculan las raciones en la ventana de 7 días que termina en esa comida. Se avisa si algún grupo de la receta **supera** su límite; llegar justo a él no es un exceso.
  - **Círculos:** las raciones al final del intervalo que se muestra. Por eso pueden verse más vacíos que los avisos de un día concreto: lo comido hace días va saliendo de la ventana.
- **Solo cuenta lo que aún puede pasar:** una comida cuyo momento ya ha pasado no suma, porque lo que no se ha registrado no se sabe si se comió. Los consumos son la verdad.
- **No influye en «¿Qué toca hoy?» ni en «Mi alimentación»**, que siguen hablando de lo comido. Tampoco las sugerencias del modal tienen en cuenta el resto del plan: para eso está el aviso de exceso.
- **Recetas que dejan de estar disponibles:**
  - Si se despublica, se oculta o se elimina de forma lógica, la comida sigue en el plan con `disponible: false`, sin contar. Así el usuario se entera, en vez de que desaparezca sin más.
  - Si se borra del todo (nadie la había comido), la comida desaparece con ella: `ON DELETE CASCADE`.
- **«Ya la he comido»:** crea el consumo y borra la comida en una transacción, con `SELECT … FOR UPDATE` para que un doble clic no lo registre dos veces. Un consumo no puede ser futuro, así que si aún no ha llegado su momento se registra con la hora actual.
- **Límites:** hasta 90 días vista y hasta 31 días por consulta.
- **Datos personales (D-064):** la descarga incluye `comidasPlanificadas`, y la baja las borra.

## D-069 · Imágenes recodificadas y en varios tamaños
- **Fase:** mejoras, bloque 4
- **Motivo:** se publicaba el fichero subido tal cual. Una foto de móvil de 5 MB se descargaba entera en cada tarjeta y conservaba sus metadatos, incluida la ubicación GPS, a la vista de cualquiera.
- **Al subirla:** se comprueba el tipo real (D-034) y la imagen se recodifica a JPEG (calidad 0,82, progresivo):
  - se aplica la orientación del EXIF, para que las fotos de móvil no salgan giradas;
  - el lado mayor se limita a 1600 píxeles, sin agrandar nunca;
  - lo transparente de un PNG o un WebP se ve blanco;
  - no se conservan los metadatos ni el fichero original.
- **JPEG y no WebP**, aunque el plan de mejoras decía WebP. El JDK no sabe escribir WebP y las librerías que lo hacen llevan código nativo por plataforma, lo que complica la imagen Docker. Se aceptan WebP de entrada; solo la salida es JPEG. Thumbnailator (escalado y orientación) y TwelveMonkeys (lectura de WebP y de JPEG CMYK) son Java puro.
- **Versiones reducidas:** `?ancho=400|800|1200`. Se generan la primera vez que se piden y se guardan en `UPLOAD_DIR/variantes`.
  - Bajo demanda, y no al subirla, para que valga también para las imágenes anteriores a este cambio y para las de demostración, sin migrarlas.
  - Los anchos son fijos: aunque la ruta es pública, el trabajo se limita a tres ficheros por imagen.
  - Si no se puede generar o la imagen ya es más estrecha, se sirve la guardada.
  - El frontend usa `srcset` y `sizes` para que el navegador elija según la pantalla.
- **Protección:** antes de descomprimir se leen las dimensiones de la cabecera y se rechazan las de más de 64 megapíxeles: un fichero pequeño puede ocupar gigas en memoria al abrirse (desde D-089, las de más de 25, y como mucho dos imágenes a la vez).
- **Varios servidores:** las versiones, igual que las imágenes, viven en `UPLOAD_DIR`, que tendría que estar compartido.
- **Rendimiento en móvil:** la medición con Lighthouse mostró que la nota no dependía de las imágenes (LCP 0,4 s, bloqueo 0 ms), sino de los saltos de contenido: el pie estaba a la vista al cargar y los datos, al llegar, lo empujaban. Se corrige reservando el espacio: el contenido principal ocupa al menos la pantalla, y la portada reserva el sitio de las tarjetas mientras cargan. Con eso, en una medición local a tamaño móvil, el rendimiento de la portada, el listado y el detalle pasó de 86, 77 y 91 a 100.

## D-070 · Sesión en cookie HttpOnly y anulación de sesiones
- **Fase:** mejoras, bloque 4
- **Sustituye a D-046** y corrige las consecuencias de D-019.
- **Motivo:** con el JWT en `localStorage`, un XSS podía llevárselo y usarlo hasta 8 horas desde otro sitio, y cambiar la contraseña no echaba a quien lo tuviera.
- **Cookie `toca_sesion`:** `HttpOnly`, `SameSite=Strict`, `Path=/api`, con la duración del token.
  - `Secure` se controla con `COOKIE_SEGURA`: por defecto `true`, y `false` en `dev`, en las pruebas y en el Docker Compose de ejemplo, que sirven por HTTP.
  - El cuerpo del login ya no lleva el token, solo `caduca` y los datos del usuario, que es lo que el frontend guarda en `localStorage` para pintar la interfaz. Ese dato no autoriza nada.
  - Cerrar sesión llama a `POST /api/auth/logout`, porque el JavaScript no puede borrar una cookie `HttpOnly`. Si no hay conexión, la sesión se cierra igualmente en el dispositivo, y la cookie caduca sola.
- **CSRF:** una cookie se envía sola, así que hace falta protección, que con el token en una cabecera no era necesaria. Dos capas:
  - `SameSite=Strict`, sin coste para la aplicación: el HTML es público y las peticiones a la API salen de la propia página;
  - token CSRF de Spring Security (`csrf.spa()`): cookie `XSRF-TOKEN` y cabecera `X-XSRF-TOKEN`, que el `HttpClient` de Angular envía sin código adicional.
  - El token no rota con cada petición. Spring lo cambia al detectar un inicio de sesión, y con JWT cada petición lo parece, así que se desactiva esa estrategia: si no, una petición lanzada mientras llega la respuesta de otra podía recibir 403.
  - Solo se exige a las escrituras autenticadas con la cookie. Con `Authorization` no hace falta: el navegador no añade esa cabecera por su cuenta. Sin sesión tampoco: la respuesta sigue siendo 401, no 403. `/api/auth/**` no lo exige, porque ahí aún no hay sesión.
- **Se sigue aceptando `Authorization: Bearer`** para clientes que no son un navegador, Swagger UI y los tests.
- **Anulación de sesiones:** `usuarios.VERSION_SESION` (V9) y el claim `ver` del token. La comprobación de cada petición (D-063) compara las dos sin ninguna consulta extra.
  - Sube al cambiar o restablecer la contraseña y al cambiar el rol. Quien tenía la sesión abierta recibe 401 en su siguiente petición.
  - Quien cambia su propia contraseña recibe una cookie nueva y sigue dentro; sus otros dispositivos salen.
  - Cerrar sesión no sube la versión, para no echar a los demás dispositivos del usuario.
- **No se han usado tokens cortos renovables:** el plan los daba como alternativa. Con la cookie y la versión de sesión se resuelve el riesgo sin añadir un segundo tipo de token ni su rotación.
- **Al desplegar este cambio:** las sesiones abiertas se cierran una vez, porque los tokens antiguos no llevan versión.

## D-071 · Límite de intentos compartido entre servidores
- **Fase:** mejoras, bloque 4
- **Sustituye al almacenamiento en memoria de D-022.** Con la ventana en memoria, cada servidor llevaba su cuenta: con varios, un atacante tenía tantos intentos como servidores, y reiniciar la aplicación ponía el contador a cero.
- **En MySQL y no en Redis:** la aplicación ya depende de la base de datos para iniciar sesión, y solo se escribe en login y en «he olvidado mi contraseña». Añadir otro servicio que desplegar no compensa.
- **Tabla `limite_intentos`** (V10): una fila por clave con el fin de la ventana y los intentos.
  - Una sola sentencia `INSERT … ON DUPLICATE KEY UPDATE` suma el intento o abre una ventana nueva. Es atómica: dos servidores a la vez no pierden intentos, sin transacción ni bloqueos explícitos.
  - Las reglas no cambian: ventana fija por IP, los mismos límites y 429 con `Retry-After`.
- **Limpieza:** cada 1000 intentos, cada servidor borra hasta 1000 ventanas terminadas. No hace falta una tarea programada, y una fila vieja no molesta: el siguiente intento de esa clave la reutiliza.
- **Clave recortada a 100 caracteres:** detrás de un proxy, la IP sale de `X-Forwarded-For`, un texto que puede venir manipulado.

## D-072 · Actualizaciones automáticas de dependencias
- **Fase:** mejoras, bloque 5
- **Dependabot y no Renovate:** viene integrado en GitHub y no hay que instalar ninguna aplicación en el repositorio. Su configuración es `.github/dependabot.yml`.
- **Qué vigila:** las dependencias npm del frontend, las de Maven del backend (también las versiones en propiedades del `pom.xml` y el padre de Spring Boot), las acciones de la CI y los servicios de `docker-compose.yml`.
  - El `Dockerfile` queda fuera: sus imágenes base (`node:24-alpine`, `eclipse-temurin:25-jdk`) llevan solo la versión mayor y reciben los parches en cada build. Dependabot únicamente propondría saltos de versión mayor. **Cambiado en D-090:** van fijadas por digest y Dependabot propone el digest nuevo de cada etiqueta.
  - Tampoco ve la versión de Node que descarga el perfil `frontend` (`nodeVersion` del `pom.xml`), la del Maven Wrapper (desde D-090, con su SHA-256) ni la de Mailpit en `scripts/mailpit.ps1`, con su SHA-256: se suben a mano. La de Mailpit, a la vez que la de `docker-compose.yml`, con el SHA-256 que publica GitHub para `mailpit-windows-amd64.zip`.
- **Cuándo:** los lunes a las 6:00 (hora de Madrid), y solo versiones publicadas hace al menos una semana (`cooldown`). Así no se adopta un paquete publicado por error o comprometido, que suele retirarse en pocos días.
- **Grupos:** un pull request por grupo, no uno por paquete.
  - **Angular** (`@angular/*`, angular-eslint): sus paquetes exigen la misma versión exacta entre ellos (D-057), así que tienen que subir juntos. Es lo que pasó al subir a mano: instalar los paquetes uno a uno falla con `ERESOLVE`.
  - **TipTap**, **Font Awesome**, **Vitest** (con `@vitest/coverage-v8`) y **ESLint** van juntos por la misma razón.
  - El resto, en un grupo de desarrollo y otro de producción, solo con versiones menores y parches. Una versión mayor llega en su propio pull request, para revisarla por separado.
- **Lo que no se sube solo:**
  - las versiones mayores de Angular y de Spring Boot: se hacen con `ng update` (que aplica las migraciones del código) o a mano (D-001);
  - las de ng-bootstrap y angular-eslint, porque cada una exige la siguiente versión mayor de Angular;
  - TypeScript fuera de la 6.0.x, que es la que admite Angular 22;
  - MySQL fuera de la 8.4, la versión LTS (las 9.x son *innovation*).
- **Comprobación:** cada pull request pasa la CI completa. La CI no usa secretos del repositorio (D-060), así que funciona igual en los pull requests de Dependabot, que no tienen acceso a ellos.
- **Primera tanda** (noviembre de 2026): cinco pull requests, todos con la CI en verde.
  - Se revisaron las notas de cada versión antes de aplicarlas. La única con correcciones de seguridad que nos afectan es la del saneador OWASP (20261103.2): una etiqueta con miles de atributos tardaba un tiempo cuadrático. Las demás correcciones de esa versión son de SVG, `srcset` y otros atributos que la política de D-035 no permite, así que el HTML guardado no cambia.
  - Se aplicaron juntos sobre `main` (`cherry-pick`), con la verificación completa en local y un solo push. Dependabot cierra los pull requests al ver las dependencias al día.
  - TipTap llegó en el grupo `produccion` y no en el suyo, pero con las tres piezas juntas, que es lo que importa.
- **Alertas de seguridad:** son aparte. Para recibir también los pull requests que corrigen vulnerabilidades conocidas, hay que activar *Dependabot alerts* y *Dependabot security updates* en *Settings → Code security* del repositorio.

## D-073 · Tipos del frontend generados desde el OpenAPI
- **Fase:** mejoras, bloque 5
- **Motivo:** `modelos.ts` copiaba a mano los DTO del backend. Un campo nuevo, renombrado o que pasaba a admitir `null` solo se notaba al fallar en el navegador.
- **Cadena:**
  1. `docs/openapi.json` es la descripción OpenAPI de la API, versionada. `OpenApiTest` la compara con la que genera springdoc. Si no coincide, la regenera y falla, para que el cambio se revise y se suba.
  2. `npm run api` (en `frontend/`) genera `src/app/core/api.generado.ts` con openapi-typescript, también versionado.
  3. `modelos.ts` solo da a cada esquema el nombre que usa el frontend (`type RecetaResumen = Esquemas['RecetaResumenDto']`). Se escribe a mano solo lo que no es de la API, como `FiltroRecetas`.
  4. Los cuerpos de las peticiones se comprueban con `satisfies Esquemas['ConsumoRequest']`.
  5. La CI comprueba las dos mitades: `OpenApiTest` en el trabajo del backend y, en el del frontend, que `npm run api` no cambia `api.generado.ts`.
- **Obligatorios y nulos:** springdoc solo marcaba como obligatorios los campos con `@NotNull` o `@NotBlank`. En los tipos generados, todos los campos de las respuestas habrían sido opcionales.
  - Jackson escribe siempre todos los campos de un `record`, también los nulos. `EsquemasDeRecords` (un `ModelConverter` de swagger-core) marca todos como obligatorios, también los de un `@JsonUnwrapped`.
  - Los que admiten `null` llevan `@Nullable` de JSpecify, que ya llega con Spring 7. En OpenAPI 3.1 quedan como `"type": ["string", "null"]`, y en TypeScript como `string | null`. Solo se anotan los DTO de la API, porque son los que definen el contrato.
  - Se aplica también a las peticiones: un campo opcional se envía como `null` explícito. Afecta a `fecha` al registrar un consumo y a `motivo` al volver a mostrar una receta, que antes se omitían. El backend acepta las dos formas.
  - `dificultad` y `roles` declaran sus valores (`@Schema(allowableValues)`), para que el frontend conserve sus uniones (`'Fácil' | 'Media' | 'Difícil'`).
- **Una descripción estable,** para poder compararla byte a byte entre Windows y Linux:
  - claves ordenadas y salida con sangría (`springdoc.writer-with-order-by-keys` y `writer-with-default-pretty-printer`);
  - servidor fijo (`/`): si no, springdoc pone la URL de la petición;
  - `operationId` con el recurso delante (`recetaListar`). Con el nombre del método solo, springdoc numeraba los repetidos (`listar_1`) según el orden en que encontraba los controladores, que puede cambiar de un sistema a otro.
- **Correcciones de paso en la descripción:**
  - los filtros de recetas aparecían como un único parámetro `filtro` obligatorio: ahora, con `@ParameterObject`, son parámetros de consulta opcionales y Swagger UI los puede usar;
  - el esquema de los errores (`ErrorRespuesta`) no salía, porque lo escribe el manejador de errores: se añade a los componentes;
  - el texto de Swagger UI decía que el token se copia de la respuesta del login, y desde D-070 va en la cookie.
- **openapi-typescript 7 declara compatibilidad solo con TypeScript 5,** y Angular 22 exige la 6.0. Un `overrides` en `package.json` le hace usar la 6.0, sin `--legacy-peer-deps`, que relajaría las comprobaciones de todo el árbol. Genera exactamente la misma salida con las dos versiones.
- **Solo los esquemas:** los servicios siguen escritos a mano con `HttpClient`. Generarlos también traería otra forma de llamar a la API, sin los listados (`crearListado`) ni la caché que ya existen, y sin ventaja clara.
- `api.generado.ts` no pasa por ESLint, porque no se escribe a mano; Prettier sí lo formatea.

## D-074 · Cobertura mínima de los tests del frontend
- **Fase:** mejoras, bloque 5
- **Herramienta:** el proveedor V8 de Vitest (`@vitest/coverage-v8`), en la misma versión que Vitest. Dependabot los sube juntos (D-072).
- **Qué se mide:** todos los `.ts` y `.html` de `src/app`, salvo los tests y `api.generado.ts`. Las plantillas cuentan porque el builder de Angular las relaciona con su código.
  - Hay que decirlo expresamente: por defecto, Vitest solo mide los ficheros que algún test carga. Así, «Mis recetas», que no tenía ningún test, ni siquiera aparecía en el informe.
- **Umbrales globales** en `angular.json`: sentencias 85 %, ramas 80 %, funciones 75 % y líneas 85 %.
  - Quedan algo por debajo de lo medido al fijarlos (88,8 %, 86,1 %, 76,8 % y 90,5 %): frenan un retroceso sin fallar por una refactorización pequeña. Se suben cuando mejore la cobertura.
  - Las funciones están más bajas porque cada manejador de evento de una plantilla cuenta como una función.
  - No hay umbral por fichero: obligaría a escribir tests de poco valor para componentes casi sin lógica.
- **Comandos:** `npm test` sigue sin medir la cobertura, para ir más rápido al desarrollar. `npm run test:cobertura` la mide, aplica los umbrales y deja el informe HTML en `frontend/coverage`. La CI usa este último y sube el informe como artefacto, igual que el de JaCoCo.
- **Tests añadidos** donde faltaban y aportaban algo:
  - **rutas:** cada pantalla carga su componente y tiene título. Un `import()` diferido roto solo se descubría al abrir esa pantalla en el navegador;
  - **«Mis recetas»:** no tenía ninguno (publicar, editar y eliminar desde la tarjeta);
  - **búsqueda de ingredientes del typeahead:** espera, filtro de los ya elegidos y errores;
  - **avisos:** estilo, rol de región viva y cerrar.

## D-075 · Accesibilidad: pruebas automáticas, foco al cambiar de pantalla y revisión con NVDA
- **Fase:** mejoras, bloque 5
- **Motivo:** Lighthouse dio 100 en accesibilidad (D-059), pero fue una medición puntual y no detecta todo: no abre modales, no provoca errores de formulario y no comprueba adónde va el foco.
- **axe-core en las pruebas e2e** (`e2e/accesibilidad.e2e.ts`, con `@axe-core/playwright`): reglas WCAG 2.2 A y AA y buenas prácticas.
  - Recorre las pantallas sin sesión, las de usuario con datos (receta, «me gusta», consumo y comida planificada) y las de administración.
  - También con los modales abiertos (nuevo ingrediente, planificar, editar consumo, confirmar eliminación, eliminar la cuenta, nuevo grupo, fusionar y ocultar), con el menú de usuario abierto, con errores en el registro y en móvil.
  - Espera a que no quede nada cargando ni a mitad de una animación: durante un fundido de animate.css, el contraste aún no es el definitivo.
  - **Encontró un fallo:** el botón «Ya la he comido» del planificador se llamaba «Ya he comido {receta}» para los lectores de pantalla. El nombre accesible debe contener el texto visible (WCAG 2.5.3): quien usa control por voz dice lo que ve. Ahora es «Ya la he comido: {receta}».
- **Solo con teclado** (`e2e/teclado.e2e.ts`): «Saltar al contenido» es lo primero, el login y el menú funcionan con el teclado, un modal retiene el foco y lo devuelve al botón que lo abrió, y el menú de usuario se abre, se recorre y se cierra con Escape.
- **Foco al cambiar de pantalla** (`FocoAlNavegar`):
  - En una aplicación de una sola página el navegador no carga una página nueva. Al pulsar un enlace, el foco se quedaba en él, o se perdía si el enlace desaparecía, y un lector de pantalla no anunciaba nada.
  - Ahora el foco pasa al `h1` de la nueva pantalla, con `tabindex="-1"` y sin marco, porque no es interactivo. Si el título aún no está, porque depende de datos que se están cargando, pasa al contenido principal.
  - No se hace en la primera carga, que el navegador y el lector ya tratan como una página nueva, ni cuando solo cambian la consulta o el ancla.
  - No cambia el desplazamiento (`preventScroll`): de eso se encarga el router (`scrollPositionRestoration`).
- **Revisión con NVDA** (2026.3.1, con Edge, sobre los datos de demostración):
  - **Método:** NVDA portátil, sin instalar, con el sintetizador «silencio» y su registro al nivel *input/output*, que recoge todo lo que anuncia. Un script recorre la aplicación en un Edge visible: inicio de sesión, portada, menú, «¿Qué toca hoy?», «Mi alimentación», apuntar un consumo, el modal del planificador y el formulario de receta. Combina Playwright con pulsaciones de teclado del sistema, porque NVDA no ve las teclas que Playwright envía dentro del navegador.
  - **Lo que funcionaba:**
    - regiones (cabecera, navegación, principal, búsqueda, pie) y encabezados en orden;
    - el foco al título al cambiar de pantalla, ya con `FocoAlNavegar`;
    - los errores de formulario, que se anuncian al salir del campo y al volver a él («entrada inválida, Este campo es obligatorio»);
    - el error de inicio de sesión, como alerta;
    - los modales, anunciados como diálogo con su título y con el foco en su primer campo;
    - el estado de los botones del editor.
  - **Corregido:**
    - **Cifras pegadas a su texto oculto:** NVDA leía «60 minde preparación», «4comensales» y «2me gusta». El navegador suprime el espacio final antes de un texto oculto con `visually-hidden`. Ahora la cifra visible lleva `aria-hidden` y el texto oculto dice la frase completa: «60 minutos de preparación», «4 comensales», «2 me gusta».
    - **Avisos que no se anunciaban:** al apuntar un consumo, NVDA no decía nada. Cada toast de ng-bootstrap es una región viva que aparece a la vez que su texto, y los lectores de pantalla solo anuncian los cambios de una región que ya existía. `Avisos` repite el texto en dos regiones ocultas que existen desde el arranque, una cortés y otra urgente para los errores: las de `Anunciador` (ver la comprobación, más abajo). Los toasts quedan como algo visual (sin `role` y con `aria-live="off"`), para que nada se anuncie dos veces.
    - **Anuncio en inglés:** el autocompletado de ng-bootstrap anuncia «1 result available», un texto fijo que no se puede configurar, porque su servicio `Live` no es público. `AnunciosNgBootstrap` lo traduce («1 resultado», «Ningún resultado») con un `MutationObserver`, que se ejecuta antes de que el navegador actualice el árbol de accesibilidad.
    - **Foco perdido en el planificador:** al planificar, quitar o marcar una comida como comida, el botón pulsado desaparece y el foco caía al principio de la página, sin ningún aviso. Ahora el foco pasa al hueco del día (`<li tabindex="-1">`), que el lector lee entero, y un aviso confirma la acción («… planificada: comida del viernes 13 nov.», «… quitada del planificador.»).
  - **Comprobación de los arreglos** (otras dos pasadas, solo sobre lo corregido): NVDA confirma las cifras («45 minutos de preparación», «4 comensales», «3 me gusta»), el aviso al apuntar un consumo, «1 resultado» en español y el foco en el hueco del planificador, con su aviso. Encontró un fallo más:
    - **Un modal ocultaba los anuncios:** al abrirse, `NgbModal` pone `aria-hidden="true"` a todo lo que cuelga de `<body>` fuera de él, también a `app-root`, y no lo quita hasta que termina la animación de cierre.
      - NVDA solo anunciaba la primera búsqueda del modal del planificador: entonces `#ngb-live` aún no existía al abrirlo; en la segunda, ya estaba oculta.
      - El aviso que sale al guardar un modal («Consumo cambiado al…») se escribía unos 200 ms antes de que `app-root` dejara de estar oculto. Afectaba a todos los modales que terminan con un aviso: ingredientes, grupos, consumos, eliminar o ocultar una receta.
      - Ahora las regiones de los avisos son de `Anunciador` (`core/anunciador.ts`): hijas directas de `<body>`, creadas al arrancar. Un `MutationObserver` les quita el `aria-hidden`, y también a `#ngb-live`, antes de que el navegador actualice el árbol de accesibilidad (`mantenerAccesible`). Siguen fuera del modal, pero solo contienen el último anuncio y están ocultas a la vista.
      - Comprobado con NVDA: se anuncian el aviso al guardar el modal del histórico y las dos búsquedas seguidas.
    - **Anuncios repetidos:** por precaución, un anuncio igual al anterior lleva al final un espacio de no separación, que no se pronuncia (`anuncioDistinto`). Vaciar la región antes de escribir podría no bastar si el navegador agrupa los dos cambios. Con NVDA se anuncian dos avisos iguales seguidos; no se ha visto que fallen sin él.
  - **Lo que no se ha cambiado:**
    - **Campos obligatorios:** no se anuncian como tales; solo se marca «Opcional» en los que no lo son, y el lector lo lee igual que se ve. Añadir `aria-required` a todos los campos sería una mejora.
    - **Barra del editor:** sus diez botones son diez paradas de Tab. El patrón de barra de herramientas usa una sola parada y las flechas para moverse dentro de ella.
    - Al pulsar Escape dentro de un campo, NVDA sale primero del modo foco: hace falta un segundo Escape para cerrar el modal. Es el comportamiento normal de NVDA, no de la aplicación.

## D-076 · Lista de la compra e impresión en PDF
- **Fase:** mejoras, bloque 6 (ampliaciones de la memoria, §13: «exportación a PDF» y, en parte, «compra de ingredientes»)
- **Qué:** la lista de la compra de las comidas planificadas (`GET /api/plan/compra`, pantalla `/planificador/compra`) y una versión para imprimir o guardar en PDF de la lista y de cada receta.
- **Qué comidas entran:** las de la semana que se ve en el planificador cuyo momento aún no ha pasado. Lo pasado ya se ha comprado o no se va a cocinar. Las comidas con la receta no disponible (despublicada, oculta o eliminada) no entran, y la lista dice cuántas son.
- **Orden:** por grupo de alimentos, como los pasillos de una tienda, con los grupos básicos (D-054: sal, aceite, especias) al final, marcados como «suele haber en casa». No se quitan: a veces se acaban.
- **Cantidades:** son texto libre (`ingrediente_receta.CANTIDAD`), así que solo se suman con reglas prudentes (`CantidadesCompra`):
  - todas deben empezar por un número (entero, decimal con coma o punto, con punto de miles, o fracción: «1 1/2») y seguir con una palabra o nada;
  - la unidad debe ser la misma, en singular o en plural y sin tener en cuenta las tildes («1 limón» y «2 limones»); el resto de la unidad debe coincidir («dientes de ajo»);
  - el total va en singular si es 1 y en plural si no, con la forma que se haya visto o, si no, formada con las reglas del español; las abreviaturas («kg», «ml») no cambian;
  - no se convierten unidades («250 gramos» y «1 kg» no se suman), ni se escala por comensales: la lista pide lo que dice cada receta.
  - Si no se puede sumar, `total` es `null` y la lista muestra solo el desglose por receta. El desglose se ve siempre que el ingrediente salga de más de una comida, para que se pueda comprobar la suma.
- **Marcas («ya lo tengo»):** en `localStorage`, solo en ese navegador, que basta para ir a la compra con el móvil.
  - Van por ingrediente y caducan a los 7 días, no por semana: la semana del planificador empieza hoy y cambia cada día, y lo marcado el domingo se perdería el lunes.
  - Se borran al iniciar y al cerrar sesión, con los datos guardados para usar sin conexión (`borrarDatosGuardados`), porque no distinguen usuarios.
  - Guardarlas en el servidor permitiría compartirlas entre dispositivos, pero exigiría una tabla nueva para un estado de usar y tirar.
- **PDF con el navegador, no en el servidor:**
  - El botón **Imprimir o guardar en PDF** abre el diálogo de impresión, que en todos los navegadores actuales permite **Guardar como PDF**.
  - Generar el PDF en el servidor exigiría una librería (OpenPDF, openhtmltopdf…), sus fuentes y una maquetación aparte, y el PDF del navegador ya conserva el texto y los enlaces.
  - Unos estilos `@media print` dejan solo el contenido: fuera la cabecera, el pie, los avisos y los botones (`d-print-none`), y los enlaces como texto.
  - La receta va en una columna, con los ingredientes antes de las instrucciones, como en el móvil, y sin el formulario de consumo ni los «me gusta». La lista va a dos columnas, sin partir un grupo entre páginas, con una casilla de borde negro junto a cada ingrediente y lo marcado tachado: el navegador no imprime los fondos.
- **La semana, en la URL:** el planificador lleva `?semana=` (sin entrada nueva en el historial) y la lista lo recibe, así que «Lista de la compra» y «Volver al planificador» abren la misma semana.
- **Sin conexión:** la respuesta entra en el `dataGroup` de `/api/plan` del service worker (D-057), así que la lista abierta con conexión se puede volver a ver en la tienda sin cobertura; las marcas son locales.

## D-077 · Valoraciones con nota y comentarios
- **Fase:** mejoras, bloque 6 (ampliaciones de la memoria, §13: «valoraciones con nota y comentarios»)
- **Decidido por:** usuario, que los «me gusta» y las valoraciones convivan.
- **Qué:** cada usuario puede poner a una receta una nota de 1 a 5 y un comentario opcional, y cambiarlos o borrarlos (tabla `valoracion_receta`, migración V11). Las recetas llevan su nota media y cuántas valoraciones tienen, en las tarjetas y en el detalle.
- **Conviven con los «me gusta»:** el «me gusta» es un favorito personal y alimenta «Recetas que me gustan» (D-062); la valoración es la opinión pública. Se cambia lo que significa **Mejor valoradas** en la portada: antes, más «me gusta»; ahora, mejor nota.
- **Mejor valoradas, por media ponderada:** cada receta cuenta como si tuviera además dos valoraciones de 3, `(suma + 6) / (n + 2)`. Así, una sola nota de 5 (3,67) no adelanta a tres notas de 4, 4 y 5 (3,8), y una receta sin valoraciones queda en 3. En empate, más «me gusta» y después la más reciente.
- **Reglas:**
  - Una valoración por usuario y receta (`UNIQUE`). Guardarla de nuevo la sustituye (`INSERT … ON DUPLICATE KEY UPDATE`), con la fecha de la última vez.
  - Nadie valora su propia receta (403 `PROPIA_RECETA`). Las demás reglas salen de la visibilidad (D-032): quien no es su autor solo ve una receta publicada, no oculta y no eliminada, así que solo esas se valoran.
  - No hace falta haberla comido: comprobarlo dejaría fuera a quien la prepara fuera de la aplicación sin apuntarla.
  - El comentario es **texto plano**, de hasta 1000 caracteres: no se admite HTML (no hace falta el saneador de D-035) y se muestra siempre como texto, respetando los saltos de línea.
- **Moderación:** un administrador puede ocultar una valoración desde la propia receta (`PUT /api/admin/valoraciones/{id}/oculta`, con el rol comprobado en la base de datos, D-043).
  - Oculta, no se lista ni cuenta en la media. Los administradores la siguen viendo, marcada, para poder mostrarla de nuevo.
  - Su autor la ve en su formulario con un aviso. Cambiarla **no** la vuelve a mostrar, para que no baste con editarla para saltarse la moderación; borrarla sí la quita.
  - No hay motivo, a diferencia de las recetas (D-063): un comentario suele ser corto y el aviso basta.
- **Cuenta (D-064):** la descarga de datos incluye las valoraciones propias, también las ocultas, y la baja las borra. Las que otros pusieron a sus recetas se borran con ellas o, si la receta se conserva eliminada (D-061), quedan, pero nadie las ve.
- **Accesibilidad (D-075):**
  - Las estrellas son decorativas (`aria-hidden`). El lector de pantalla lee un texto oculto con todo, «Nota media: 4,5 de 5, 12 valoraciones», y la cifra visible de al lado también va oculta para él. Si no, la leería pegada al texto oculto, como pasaba con las cifras de las tarjetas.
  - Para elegir la nota hay cinco botones de opción («4 estrellas») dentro de un `fieldset`, ocultos a la vista pero no al teclado: las flechas cambian la nota y el foco se ve en la estrella.
  - Al borrar la propia, el botón desaparece y el foco pasa al formulario.
  - El dorado de las estrellas (`#9a6a00`) tiene un contraste de unos 4,7:1 sobre blanco.
- **No se imprimen** (D-076): la sección de valoraciones lleva `d-print-none`.

## D-078 · Avisos push del planificador
- **Fase:** mejoras, bloque 6 (ampliaciones de la memoria, §13: «notificaciones push»)
- **Decidido por:** usuario, que los avisos sean solo el recordatorio de la comida planificada.
- **Qué:** una hora antes de cada comida o cena del planificador, un aviso en los navegadores en los que el usuario lo ha activado («Comida: Lentejas estofadas · Hoy a las 14:00, según tu planificador.»). Al pulsarlo se abre la receta. Se activa en **Mi perfil**, con un aviso de prueba.
- **Sin librería de Web Push:** el cifrado (RFC 8291, `aes128gcm` de RFC 8188) y la identificación VAPID (RFC 8292, JWT ES256) se hacen con la criptografía del JDK (ECDH P-256, HKDF con HMAC-SHA256, AES-GCM, `SHA256withECDSAinP1363Format`), y el envío con su `HttpClient`.
  - La librería Java habitual (`nl.martijndwars:web-push`) lleva años sin publicar versiones y arrastra Bouncy Castle y un cliente HTTP antiguo; son unas 150 líneas.
  - Se prueba con el ejemplo completo del apéndice A de RFC 8291 (mismo cuerpo, byte a byte), descifrando en el test como lo haría el navegador y verificando la firma VAPID.
  - Comprobado además con un servicio push real: Edge suscrito a WNS recibe el aviso de prueba y el recordatorio.
- **El service worker de Angular** muestra los avisos: el mensaje lleva su formato (`notification`, con `data.onActionClick` → `navigateLastFocusedOrOpen` a la receta). No hace falta un service worker propio, y la CSP no cambia.
- **Claves VAPID** en `PUSH_CLAVE_PUBLICA` y `PUSH_CLAVE_PRIVADA` (`scripts/generar-claves-push.ps1`, con la criptografía de .NET). Sin ellas los avisos están desactivados: la pantalla lo dice y la API responde 409.
- **Suscripciones** (`suscripcion_push`, migración V12): una por navegador, identificado por su endpoint, con un `UNIQUE` sobre su SHA-256, porque 1000 caracteres pasan del límite de un índice de InnoDB.
  - Guardan la zona horaria del navegador, para escribir la hora en el aviso.
  - Una que el servicio push ya no conoce (404 o 410) se borra al intentar enviarle algo.
- **Solo servicios push conocidos:** el servidor manda peticiones al endpoint que da el navegador, así que solo se admiten URL `https` del puerto estándar de FCM, Mozilla, WNS y Apple (`app.push.servicios`). Si no, un usuario podría hacer que el servidor llamara a direcciones internas.
- **El recordatorio:** una tarea programada (`@Scheduled`, cada cinco minutos) busca las comidas que empiezan dentro de la próxima hora (`app.push.antelacion`).
  - Solo de usuarios no bloqueados y con la receta aún disponible para ellos, con las reglas del planificador (D-068).
  - Cada comida se avisa **una sola vez**: la columna `AVISADA` se marca con un `UPDATE … WHERE AVISADA IS NULL`, y solo envía quien la cambia. Así no se repite aunque haya varios servidores, como el límite de intentos de D-071.
  - El aviso caduca a la hora de la comida (cabecera `TTL`): si el dispositivo está apagado, no llega tarde.
  - Si se quita la comida o se marca como comida antes, ya no está en el planificador y no se avisa.
- **Equipos compartidos:**
  - Al cerrar sesión, se borra la suscripción de ese navegador.
  - Al iniciar sesión, si el navegador recibía los avisos de otra persona (no cerró sesión), se anula su suscripción. Así nadie hereda los avisos de otro.
  - De quién es la suscripción se guarda en `localStorage`. Si vuelve a entrar la misma persona, como al caducar la sesión cada día, los avisos siguen.
  - Si otra persona activa los avisos en ese mismo navegador, la suscripción pasa a ser suya.
- **Cuenta (D-064):** la descarga de datos incluye en qué navegadores están activados, solo con el dominio del servicio (la dirección completa da acceso al dispositivo), y la baja los borra.
- **Limitaciones:**
  - Los avisos necesitan el service worker, que solo existe en el build de producción (D-057), y un navegador que los admita.
  - En iPhone y iPad, solo con la aplicación instalada en la pantalla de inicio.
  - Las pruebas e2e no pueden usar un servicio push real: comprueban la pantalla y su accesibilidad, y la entrega real se probó a mano.

## D-079 · Estudio nutricional con BEDCA
- **Fase:** mejoras, bloque 6 (ampliaciones de la memoria, §13: «estudio nutricional avanzado»)
- **Decidido por:** usuario, la fuente (BEDCA) y su uso: educativo y no comercial, con la fuente a la vista, los valores por 100 g sin cambios y los totales de las recetas presentados como estimación. Si la aplicación pasara a tener un uso comercial, habría que pedir autorización a BEDCA.
- **Qué:** el detalle de una receta muestra su **información nutricional estimada**, por ración y de la receta entera: energía (kcal y kJ), grasas, saturadas, hidratos de carbono, fibra, proteínas y sal. «Cómo se calcula» explica qué cuenta de cada ingrediente, y se cita la fuente con un enlace a bedca.net. API: `GET /api/recetas/{id}/nutricion`.
- **Fuente:** AESAN/BEDCA Base de Datos Española de Composición de Alimentos v1.0 (2010), la base pública de la Agencia Española de Seguridad Alimentaria y Nutrición. Se eligió por ser española y tener los alimentos con los nombres de aquí.
  - Para cada ingrediente del catálogo se eligió **a mano** el alimento de BEDCA más genérico, crudo o fresco si lo hay («Harina de trigo», «Patata, cruda», «Huevo de gallina fresco»). Su nombre se guarda y se muestra, para que se vea con qué se calcula.
  - Los datos se descargaron una sola vez del servicio de consulta de la web de BEDCA, a un ritmo de una petición por segundo, y van en la migración **V13** (`composicion_ingrediente`; desde V14, `alimento_bedca` e `ingrediente_alimento`, D-080). La aplicación no consulta BEDCA.
  - Sin alimento adecuado en BEDCA: **Levadura** y **Carrillada** (esta, desde D-084, con la cabezada de cerdo). Para algunos se usa el más parecido, y su nombre lo deja claro: Limón → «Zumo de limón, fresco»; Mantequilla → «Mantequilla salada»; Yogur natural → «Yogur, enriquecido, natural»; Maicena → «Almidón de maíz».
  - Los valores son los de BEDCA **sin cambios**, por 100 g de porción comestible. Donde BEDCA no da un valor (los hidratos del huevo, las grasas saturadas de algunas hortalizas…), se guarda `NULL`, no un cero inventado, y no suma. Única conversión: la energía de la pimienta, que BEDCA da en kcal, se guarda en kJ (× 4,184).
- **De la cantidad a gramos:** las cantidades son texto libre (D-076), así que solo cuentan las que se pueden convertir:
  - Un número (como en la lista de la compra: «1,5», «1/2», «1 1/2») seguido de una unidad de masa o volumen (g, kg, ml, cl, dl, l, abreviadas o con todas las letras), de una cucharada (15 g) o de una cucharadita (5 g). Un mililitro cuenta como un gramo.
  - O de una **pieza** del ingrediente con peso aproximado: unidad, diente o loncha, en singular o en plural. Un número solo («2» huevos) cuenta como unidades. Los pesos (un huevo, 50 g; un diente de ajo, 5 g; una loncha de jamón serrano, 15 g…) son una **estimación propia**, no de BEDCA, y están en `peso_pieza_ingrediente` (V13), solo para los ingredientes en los que tienen sentido.
  - Detrás de la unidad solo se admite «de …» («200 gramos de harina»). «400 gramos cocidos» no cuenta: BEDCA da las legumbres secas y saldría casi el triple.
  - No cuentan «al gusto», «1 pizca», «2-3 hojas» ni las piezas sin peso. La pantalla dice cuántos ingredientes cuentan y por qué no cuenta cada uno de los demás.
  - Las cantidades se toman como porción comestible: «500 gramos de patatas» se calcula como 500 g de patata pelada. Si se pesan con piel, la estimación sale algo alta.
  - Cuenta **todo el aceite** de la receta, aunque al freír solo se absorba una parte: en una fritura, la energía y las grasas salen muy altas (en los boquerones fritos de demostración, el aceite de freír es casi toda la energía). No se puede saber desde el texto cuánto se absorbe, así que «Cómo se calcula» lo advierte.
- **Cálculo:** cada valor por 100 g se multiplica por los gramos y se suma; la ración es el total entre los comensales. Se redondea al final: la energía sin decimales, la sal con dos y el resto con uno. La **sal** se calcula con el sodio (sal = sodio × 2,5, como en el etiquetado del Reglamento (UE) n.º 1169/2011), porque es lo que se lee en las etiquetas.
- **Visibilidad:** la de la receta (D-032); los administradores, también las ocultas, como en el detalle.
- **Ingredientes nuevos:** los que crean los usuarios no tienen composición, así que no cuentan («no hay datos de su composición»). Al fusionar o borrar un ingrediente (D-065), su composición se borra con él (`ON DELETE CASCADE`); si el que queda no tenía, tampoco la tendrá. Desde D-080, la administración les asigna un alimento de BEDCA.
- **Avisos:** la sección y la página de información legal dicen que es una **estimación orientativa**, que no es una etiqueta nutricional ni un consejo médico, y citan la fuente.

## D-080 · Catálogo de BEDCA y alimentos asignados por la administración
- **Fase:** mejoras, bloque 6 (continuación de D-079)
- **Decidido por:** usuario, que los alimentos entre los que elige la administración sean el **catálogo completo de BEDCA**, guardado en la aplicación, frente a consultar BEDCA en vivo o escribir los valores a mano. Con las mismas condiciones de D-079: uso educativo y no comercial, valores sin cambios y la fuente citada.
- **Por qué:** con D-079 solo tenían composición los ingredientes del catálogo inicial; los que crean los usuarios no contaban nunca en la información nutricional.
- **Catálogo (migración V14):** `alimento_bedca` guarda los 957 alimentos de BEDCA con detalle público, con su identificador de BEDCA y los mismos siete valores por 100 g de D-079. De los 969 que lista BEDCA, de 12 no publica el detalle (siete cortes de vacuno, la lactosa, un pistacho, el puerro en conserva…), y no están.
  - Se descargaron una sola vez, como en D-079, con una petición cada vez y al menos un segundo entre ellas. La aplicación sigue sin consultar BEDCA.
  - `ingrediente_alimento` guarda el alimento de cada ingrediente (como mucho uno) y sustituye a `composicion_ingrediente` de V13: los 75 ingredientes que ya tenían composición apuntan ahora a su alimento, con los mismos valores. Así un dato de BEDCA no está copiado dos veces.
  - Valores sin cambios, como en D-079: `NULL` si BEDCA no da el dato, si no es un número o si viene en una unidad que no es la de ese componente (cuatro valores, hidratos o fibra en mg: no se adivina la conversión). La energía que BEDCA da en kcal (ocho alimentos, entre ellos la pimienta) se guarda en kJ.
- **Administración:** en **Ingredientes**, cada tarjeta tiene el botón **Datos nutricionales**, que abre un modal:
  - Dice qué alimento usa el ingrediente ahora, o que no tiene datos.
  - Busca en BEDCA por palabras, en cualquier orden y sin distinguir mayúsculas ni acentos («pollo crudo» encuentra «Pollo, entero, con piel, crudo»), y muestra los valores por 100 g del elegido antes de guardarlo.
  - **Quitar los datos** deja el ingrediente sin composición.
  - API: `GET /api/admin/alimentos-bedca?q=` (hasta 10) y `PUT /api/admin/ingredientes/{id}/alimento` con `{ "alimento": id | null }` (desde D-081, `/nutricion`, junto con las piezas). Como el resto de la administración, comprueba el rol en la base de datos (D-043) y empieza bloqueando el ingrediente (D-061).
- **Saber cuáles faltan:** el listado de ingredientes dice en cada tarjeta su alimento de BEDCA o «Sin datos nutricionales», y los administradores tienen el filtro **Solo los que no tienen datos nutricionales** (`sinAlimento=true`). Las respuestas de ingredientes llevan el campo `alimento` (su nombre, o `null`).
- **Al fusionar** (D-065), el ingrediente que queda hereda el alimento del duplicado si no tenía uno, y los pesos de sus piezas (D-079) que no tenga. Si ya tenía alimento, se queda con el suyo.
- **Los pesos de las piezas** («unidad», «diente», «loncha») venían solo de la migración V13. Desde D-081, la administración los edita en el mismo modal.

## D-081 · Pesos de las piezas desde la administración y números con letras
- **Fase:** mejoras, bloque 6 (continuación de D-079 y D-080)
- **Por qué:** los pesos de las piezas solo venían de la migración V13, así que en un ingrediente nuevo «2 unidades» o «una lata» no contaba en la información nutricional. Y cantidades como «media» o «medio kilo» no se entendían.
- **Datos nutricionales de un ingrediente:** el modal de la administración (D-080) edita ahora, juntos, el alimento de BEDCA y los **pesos de las piezas**: una palabra («unidad», «lata», «manojo») y lo que pesa en gramos de parte comestible.
  - Se guardan juntos y en una transacción: `GET` y `PUT /api/admin/ingredientes/{id}/nutricion` con `{ "alimento": id | null, "piezas": [ { "pieza": "lata", "gramos": 60 } ] }`, que sustituye a los que tenía. Reemplaza a `PUT /api/admin/ingredientes/{id}/alimento` de D-080, que solo usaba esta aplicación.
  - Validación: como mucho 10 piezas; cada una, una sola palabra de letras (se guarda en minúsculas) y un peso de más de 0 g y hasta 10.000 g, con un decimal como mucho. No puede haber dos que sean la misma en singular y en plural («lata» y «latas»), ni una unidad que ya se convierte sola («gramo», «cucharada»): no se usaría nunca.
  - Los errores de las piezas llegan por campo (`piezas[0].gramos`), y el frontend los pone en su control: `aplicarErroresServidor` convierte esos nombres en las rutas de Angular (`piezas.0.gramos`), lo que sirve para cualquier formulario con listas.
  - Mientras se cargan los datos, o si no se han podido cargar, no se puede guardar: se borrarían los que tiene. Al cargarlos, el foco pasa al buscador de alimentos.
- **Números con letras:** al principio de una cantidad, «un», «una» y «uno» valen 1, y «medio» y «media», ½: «media» cebolla es media unidad, «medio kilo» son 500 g y «una lata» es una lata. Se leen en el mismo sitio que los números (D-076), así que también los suma la lista de la compra («media» y «1» son 1,5).
- **Mensajes:** los mínimos y máximos de los formularios se escriben en español («El valor mínimo es 0,1»).

## D-082 · Nutrientes de la semana en «Mi alimentación»
- **Fase:** mejoras, bloque 6 (continuación de D-079)
- **Qué:** «Mi alimentación» tiene una sección nueva, **Nutrientes en los últimos 7 días**: la energía, las grasas, las saturadas, los hidratos, la fibra, las proteínas y la sal de lo comido, con la **media al día**, su **porcentaje de la ingesta de referencia** y el **total** de los 7 días. API: `GET /api/alimentacion/nutricion`.
- **Cálculo:**
  - La misma ventana que los indicadores (6.4, D-038): los consumos entre ahora y hace 7 días.
  - Cada consumo es **una ración** de su receta: la estimación de la receta entera (D-079) entre sus comensales, sin redondear. Se suman todas y se redondea al final.
  - La media al día es el total entre 7, aunque algún día no se registrara nada: así no sube por registrar menos días. La pantalla lo dice.
  - Los consumos de recetas sin estimación (ningún ingrediente cuenta) no suman, pero se cuentan: la pantalla dice «2 de 3 consumos tienen datos».
  - Cuentan también las recetas eliminadas o sin autor que se habían comido (D-061), porque sus ingredientes se conservan.
- **Ingesta de referencia:** la de un adulto medio del Reglamento (UE) n.º 1169/2011, anexo XIII, la de las etiquetas de los alimentos: 8.400 kJ (2.000 kcal), 70 g de grasas, 20 g de saturadas, 260 g de hidratos, 50 g de proteínas y 6 g de sal. La fibra no tiene en el reglamento, así que no se da porcentaje. La pantalla aclara que las necesidades de cada persona son distintas y que no es un consejo médico.
- **Por qué no se fijan objetivos ni colores:** lo registrado en Toca no suele ser todo lo que se come (desayunos, picoteos), así que un «te falta» o un rojo engañarían. Se muestran las cifras con su referencia y la explicación.
- **Petición propia:** la sección pide sus datos aparte del resumen (`GET /api/alimentacion/resumen`), con su carga y su reintento: si falla, el resto de la pantalla se ve igual.
- **Otros cambios:** las consultas de nutrición (`NutricionMapper`) reciben ahora varias recetas a la vez, y la de una receta (D-079) usa las mismas.

## D-083 · Galería de fotos y enlace a un vídeo en las recetas
- **Fase:** mejoras, bloque 6 (ampliaciones de la memoria, §13: «vídeos o galerías por receta»)
- **Decidido por:** usuario, el alcance: hasta 6 fotos por receta, la primera es la principal, y los vídeos como **enlaces**, no incrustados: incrustarlos obligaría a abrir la CSP de D-060 a YouTube o Vimeo y a sus cookies de seguimiento.
- **Datos (migración V15):** `foto_receta` guarda las fotos de cada receta con su orden (0, la principal) y una descripción opcional de hasta 150 caracteres, que es su texto alternativo.
  - La principal se sigue copiando en `recetas.IMAGEN`: así las tarjetas, los listados y todas sus consultas no cambian. El servicio escribe las dos cosas en la misma transacción.
  - V15 pasa la imagen que ya tenía cada receta a su galería. Si una receta tuviera imagen y ninguna fila en `foto_receta`, el servicio trata esa imagen como su única foto.
  - Los datos de demostración (`R__datos_demo.sql`) crean la galería de las recetas demo, y la tortilla tiene dos fotos más (placeholders propios, `scripts/generar-imagenes-demo.ps1`).
- **API:** el alta y la edición siguen siendo multipart (D-036), con una lista nueva en la parte `receta`:
  - `fotos`: la galería completa, en orden. Cada foto es una que ya tenía la receta (`existente`, su `nombre`) o un fichero nuevo (`nueva`, su posición entre las partes `fotos` de la petición), con su `descripcion`.
  - Sin `fotos` (`null`), la galería se queda como está; con una lista vacía, se quita entera. Las que tenía y ya no están se borran tras el commit, como la imagen sustituida de D-036.
  - Se comprueba todo antes de guardar ningún fichero: que cada foto tenga un origen y solo uno, que las existentes sean de esa receta, que no se repita ninguna, que no falte ni sobre ningún fichero, y como mucho 6. Cada error va en su campo (`fotos[2].existente`), y los de la imagen en sí, en el de su foto (`fotos[2]`).
  - La parte `imagen` desaparece: solo la usaba esta aplicación. `max-request-size` sube a 32 MB (6 fotos de 5 MB), y el 413 dice ahora «Cada foto puede ocupar como mucho 5 MB» en el campo `fotos`.
  - El detalle trae `fotos` (nombre, URL y descripción de cada una); `imagen` sigue siendo la principal.
- **Detalle:** la foto que se ve, con su descripción debajo, flechas para pasar a la anterior o la siguiente (en círculo, también con las teclas de flecha) y las miniaturas, que marcan la actual con `aria-current`. Cada cambio se anuncia con `Anunciador` («Foto 2 de 3: …»). No hay carrusel automático: una imagen que cambia sola es un problema de accesibilidad (WCAG 2.2.2). Al imprimir, solo la foto que se ve.
  - Texto alternativo: la descripción; sin ella, «Foto de {título}» si es la única o «{título}: foto 2 de 3».
- **Formulario:** «Fotos» permite elegir varias a la vez (se comprueban el tipo y el tamaño de cada una), subirlas y bajarlas de puesto, quitarlas y escribir su descripción. La primera lleva la marca «Principal». Al mover o quitar una foto, el foco se queda en un sitio razonable (el mismo botón, el de al lado o el campo para añadir) y se anuncia qué ha pasado.
- **Cuenta (D-064):** la descarga de datos incluye las URL de toda la galería de cada receta. La baja borra los ficheros de todas las fotos; las recetas que se conservan sin autor se quedan sin fotos, como antes se quedaban sin imagen.
- **Eliminar una receta (D-061):** se borran todas sus fotos; si se conserva como eliminada, también sus filas de `foto_receta` y su vídeo.
- **Vídeo:** cada receta puede tener un enlace a un vídeo (`video` en la parte `receta`; columna `recetas.VIDEO`, en V15).
  - Solo `https` y solo YouTube (`youtube.com`, `m.youtube.com`, `youtu.be`) o Vimeo (`vimeo.com`, `player.vimeo.com`), sin usuario en la URL: se comprueba el servidor de la URL, no el texto, para que «https://youtube.com.ejemplo.com» no pase. El formulario lo comprueba también, y el servidor responde 400 `errores.video` si no vale. Vacío, sin vídeo.
  - En el detalle aparece como un enlace, «Ver el vídeo en YouTube», que se abre en otra pestaña (`rel="noopener noreferrer"`). No se incrusta y la CSP no cambia.
  - Va también en la descarga de datos, y se borra al eliminar la receta o la cuenta, como las fotos.

## D-084 · Alimento de BEDCA para la Carrillada
- **Fase:** mejoras, bloque 6 (pendiente de D-079 y D-080)
- **Decidido por:** usuario, que se resolviera lo que quedaba sin datos; el alimento lo elegí yo con los criterios de D-079.
- **Carrillada → «Carne de cerdo de capa blanca, cabezada, partes grasa y magra, crudo»** (BEDCA 2700), en la migración V16:
  - BEDCA tiene la carrillera de vacuno (2710), pero no publica su detalle: es uno de los 12 alimentos que no están en el catálogo (D-080).
  - La carrillada suele ser de cerdo, y la cabezada es el corte de cerdo más parecido que tiene BEDCA: de la cabeza y el cuello, con grasa y crudo. Por 100 g, 253 kcal, 19,4 g de grasas y 20 g de proteínas, valores del orden de los de una carrillada de cerdo.
  - Se descartó «Cerdo, parte sin especificar» (2261): sus 760 mg de sodio por 100 g son de un producto elaborado, no de carne fresca.
  - Una pieza, «unidad», pesa 120 g (estimación propia, como las de V13): así cuentan recetas como «4 carrilladas».
  - Si la administración ya le había asignado un alimento o esa pieza, la migración no los cambia.
- **Levadura** sigue **sin alimento**: BEDCA no tiene levadura (ni fresca ni seca) ni nada parecido, y D-079 no admite inventar valores. En las recetas aparece como «no hay datos de su composición»; la cantidad que se usa (unos gramos) apenas cambia la estimación.

## D-085 · Acceso con Google (OpenID Connect)
- **Fase:** mejoras, la ampliación del §13 que faltaba: el inicio de sesión con redes sociales.
- **Decidido por:** usuario, que se vinculen solas las cuentas con el mismo email y que quien es nuevo elija su nombre de usuario; yo, el resto.
- **Solo Google**, con OpenID Connect: es el proveedor más extendido y da el email verificado. Otro proveedor sería otro registro de cliente en `AccesoGoogleConfig`.
- **Flujo de código de autorización con PKCE, en el servidor:**
  - Lo hace Spring Security (`oauth2Login`), en una cadena de filtros aparte solo para `/oauth2/**` y `/login/oauth2/**`. El resto de la API sigue sin sesión HTTP.
  - La sesión HTTP solo guarda el `state`, el `nonce` y el verificador de PKCE durante el intercambio, y se invalida al volver de Google.
  - Spring Security valida el token de identidad de Google: firma, emisor, destinatario, caducidad y `nonce`. No se guardan tokens de Google; se piden `openid`, `email` y `profile`.
  - La sesión de Toca es la misma cookie que la del login (D-070). Como la vuelta es una redirección, `/login/google` pide sus datos a `GET /api/auth/sesion`.
- **Cómo se reconoce a alguien:** por el identificador de su cuenta de Google (`sub`), en `usuarios.GOOGLE_ID` (V17, único). El email puede cambiar en Google y en Toca.
  1. Si ese `sub` ya está vinculado, entra con esa cuenta.
  2. Si no, y Google da un email **verificado** que es el de una cuenta de Toca sin Google, la vincula y entra. Si esa cuenta ya está vinculada con otra cuenta de Google, no entra («otra-cuenta»): no se sustituye una vinculación sin que lo pida su dueño. **Cambiado en D-086:** la vinculación pide una vez la contraseña de la cuenta.
  3. Si nadie tiene ese email, empieza un registro.
  - Sin email verificado, ni se vincula ni se registra. Una cuenta bloqueada (D-063) tampoco entra con Google.
  - Vincular por el email confía en que Google ha comprobado que es de esa persona, que es lo que significa `email_verified`. A cambio, quien ya tiene cuenta no tiene que hacer nada para usar Google.
- **Registro de alguien nuevo:**
  - En `/registro/google` elige su nombre de usuario y revisa su nombre y apellidos. Vienen de Google: la primera palabra de los apellidos es el primero y el resto, el segundo.
  - El email es el de Google y no se cambia en ese formulario (después, desde «Mi perfil»).
  - Lo pendiente va en un JWT firmado con la clave de las sesiones, pero con otro destinatario (`registro-google`) y sin versión de sesión, así que nunca vale como sesión. Dura 15 minutos y viaja en la cookie `toca_registro_google` (`HttpOnly`, `SameSite=Strict`, `Path=/api/auth/google`).
  - Si al completar el registro esa cuenta de Google ya tiene usuario (un doble envío), entra con él y no crea otro. Se envía el correo de bienvenida, como en el registro normal.
- **Contraseña:** una cuenta creada con Google tiene una contraseña aleatoria, cifrada, que nadie sabe. Para cambiarla desde el perfil o para darse de baja hace falta una, así que «Mi perfil» ofrece **Enviarme el enlace**: el mismo enlace de recuperación (D-021), que solo llega al email de la cuenta. No se añadió un «poner contraseña sin la actual»: con una sesión robada se podría quitar la cuenta a su dueño.
- **Errores:** se vuelve a `/login?google=…` (`bloqueada`, `email-no-verificado`, `otra-cuenta`, `cancelado` si se cancela en Google, `error` en cualquier otro fallo), y el login lo explica.
- **Frontend:**
  - El botón **Entrar con Google** solo aparece si `GET /api/auth/proveedores` dice `google: true`. Lleva a `/oauth2/authorization/google` con una navegación completa; la página a la que volver (`returnUrl`) se guarda mientras tanto en `sessionStorage`.
  - El service worker no intercepta `/oauth2/**` ni `/login/oauth2/**`, y el proxy de `npm start` los pasa al backend.
  - El botón no lleva el logotipo de Google, para no incluir marcas de terceros (no hay iconos de marcas en el proyecto). Si la aplicación se publica, las directrices de marca de Google piden su «G»: se añadiría con sus recursos oficiales.
- **Configuración:** `GOOGLE_CLIENT_ID` y `GOOGLE_CLIENT_SECRET`, de un cliente OAuth «Aplicación web» de Google Cloud Console.
  - Sin ellas, el acceso con Google está desactivado: no hay registro de cliente ni cadena de filtros, y el botón no aparece.
  - URI de redirección autorizados: `{URL de la aplicación}/login/oauth2/code/google`. Con el JAR, `http://localhost:8080/login/oauth2/code/google`; con `npm start`, `http://localhost:4200/login/oauth2/code/google`. En producción, con `https://`: detrás de un proxy, `forward-headers-strategy: framework` del perfil `prod` hace que el servidor la construya bien.
- **Pruebas:** Google no interviene. Se comprueba la redirección a Google (con PKCE) y la vuelta se simula con el usuario de OpenID Connect que daría Spring Security (`AccesoGoogleTest`). Con Google de verdad solo se puede probar con unas credenciales.

## D-086 · Revisión de seguridad del cierre
- **Fase:** bloque de cierre. Revisión independiente (un agente sin mi contexto) del acceso con Google, la sesión y la cuenta; aquí, lo que se confirmó y cómo se resolvió.
- **Decidido por:** usuario, la vinculación con contraseña y lo del email (las dos cambian lo que se ve); yo, el resto.
- **Vincular con Google pide la contraseña una vez** (cambia D-085; desde D-088, solo si la cuenta no ha confirmado su email). Toca no comprueba los emails de sus cuentas, así que con la vinculación automática alguien podía crear una cuenta con el Gmail de otra persona (o ponerlo en su perfil) y esperar: cuando esa persona entrara con Google, quedaría vinculada a la cuenta del atacante, que conservaría su contraseña y vería todo lo que registrara.
  - Ahora, si el email de Google es el de una cuenta sin vincular, `/login/google/vincular` pide su contraseña de Toca. Quien no la sepa (la víctima de una cuenta así) la recupera con el enlace, que llega a su email, y al cambiarla se anulan las sesiones del atacante (D-070).
  - La vinculación pendiente va en un token propio (destinatario `vincular-google`, 15 minutos) y en la cookie `toca_vincular_google`, como el registro.
  - La contraseña se comprueba antes que el bloqueo, como en el login (D-063), y con el límite de intentos de las contraseñas.
  - El email tiene que coincidir exactamente (salvo mayúsculas): la colación de MySQL da por iguales `anà@` y `ana@`.
  - Se avisa por correo al dueño de la cuenta.
- **Cambiar el email pide la contraseña actual** y avisa al email anterior; el enlace de recuperación que estuviera pendiente deja de valer. Con solo una sesión abierta (un ordenador compartido) ya no se puede poner otro email, pedir el enlace y quedarse con la cuenta. Si solo cambian las mayúsculas, es el mismo email y no la pide.
  - Quien creó la cuenta con Google no sabe su contraseña: la crea antes con «Enviarme el enlace» (D-085).
- **Desvincular Google** desde «Mi perfil», con la contraseña (después será la única forma de entrar) y aviso por correo. Antes no había forma de deshacer una vinculación.
- **Límites de intentos:**
  - Se saltaban con la ruta codificada: el filtro comparaba la ruta en bruto (`/api/auth/%6Cogin`) y Spring MVC la decodifica, así que llegaba al login sin contar. Comprobado con la aplicación: 12 intentos en un minuto sin ningún 429. Ahora el filtro compara como Spring MVC (`PathPatternRequestMatcher`).
  - Cubren también las peticiones que comprueban la contraseña (vincular, perfil, cambio de contraseña, desvincular, baja; con el límite del login y su propio contador), los registros (10 por hora: cada uno envía un correo a la dirección que se escriba) y los inicios del acceso con Google (20 por minuto: cada uno crea una sesión HTTP).
  - En `prod`, `server.forward-headers-strategy` pasa de `framework` a `native`: con `framework`, Spring tomaba la IP de `X-Forwarded-For` de cualquier cliente, y cambiándola se saltaban los límites. Con `native`, Tomcat solo cree esas cabeceras si llegan de un proxy de confianza (las redes privadas y localhost; **desde D-090**, solo localhost, o `PROXIES_DE_CONFIANZA`) y lee la IP de derecha a izquierda. También marca `Secure` la cookie de la sesión HTTP detrás de HTTPS.
- **Sesión HTTP del intercambio con Google:** dura 10 minutos (antes, los 30 por defecto) y su cookie va con `HttpOnly` y `SameSite=Lax` explícitos. `Lax`, no `Strict`: la vuelta de Google es una navegación desde otro sitio y tiene que llevarla.
- **CSRF:** solo `Authorization: Bearer` evita la comprobación. Antes bastaba cualquier cabecera `Authorization`, y el filtro JWT usaba entonces la cookie: un proxy con autenticación `Basic`, que el navegador añade solo, desactivaba el CSRF.
- **Tokens:** las sesiones llevan el destinatario `sesion` y se exige; antes se distinguían de los del acceso con Google solo porque estos no llevan versión. Las sesiones anteriores dejan de valer al desplegar este cambio (hay que volver a entrar).
- **Registro con Google ya completado:** su token ya no sirve para entrar otra vez (404 `REGISTRO_GOOGLE_CADUCADO`). Antes, durante sus 15 minutos, daba una sesión nueva aunque la cuenta hubiera cambiado de contraseña después.
- **Vincular en dos peticiones a la vez:** `vincularGoogle` solo actualiza si la cuenta sigue sin vincular (`AND GOOGLE_ID IS NULL`) y, si no, responde 409.
- **Se dejan como están:**
  - El registro dice si un email ya tiene cuenta (409 `errores.email`), como pide la especificación; ahora con el límite de registros por IP. No se aplica el criterio de D-021 (el olvido de contraseña no lo dice) porque el formulario de registro tiene que poder explicar por qué no se crea la cuenta.
  - `POST /api/auth/logout` no exige CSRF: otra web podría cerrar la sesión de alguien, pero no hacer nada con ella.
  - Que Google sea quien verifica el email: `email_verified` solo es del todo fiable con `@gmail.com` o con el dominio de Google Workspace (`hd`). Con la contraseña de D-086 ya no basta un email verificado para entrar en una cuenta existente.
- **Comprobado y correcto:** los tokens de registro, vinculación y sesión no valen unos por otros; Spring valida `state`, PKCE y `nonce`; las redirecciones de la vuelta de Google son fijas y el `returnUrl` pasa por `destinoSeguro`; las cookies son `HttpOnly`, `Secure` (salvo en desarrollo) y `SameSite=Strict` con una ruta estrecha; una cuenta bloqueada no entra por ninguna vía; los enlaces de recuperación salen de `APP_BASE_URL`, no de la cabecera `Host`.

## D-087 · Bloque de cierre: criterios de aceptación, Lighthouse y dependencias
- **Fase:** bloque de cierre, después de todas las ampliaciones del §13. Repasa los criterios de aceptación (12 de la especificación) con la aplicación tal como queda. La revisión de seguridad está en D-086.
- **Lighthouse** 13.5 en Edge, como en D-059, sobre el JAR con los datos de demostración, ahora en **las 20 pantallas**: públicas (login, registro, el registro con Google, ayuda), con sesión de `lucia` (portada, recetas, detalle, formulario, mis recetas, me gustan, ingredientes, «¿Qué toca hoy?», planificador, lista de la compra, mi alimentación, histórico, perfil) y de `admin` (usuarios, grupos, recetas ocultas).

  | Categoría | Escritorio | Móvil |
  |---|---|---|
  | Accesibilidad | 100 en todas | 100 en todas |
  | Buenas prácticas | 96–100 | 96–100 |
  | SEO | 100 | 100 |
  | Rendimiento (no es criterio de aceptación) | 100 | 100 |

  - El 96 es el mismo artefacto de D-059: Lighthouse anota un 504 del service worker sobre el documento de la pantalla anterior. En `/registro/google` se suma el 404 de `GET /api/auth/google/registro` cuando no hay ningún registro pendiente, que es la respuesta prevista.
  - **Instalable como PWA:** `Page.getInstallabilityErrors` vacío y el manifest sin errores.
- **Responsive:** la prueba e2e de scroll horizontal (375, 768 y 1280 px) cubre ahora también «Me gustan», la lista de la compra, las pantallas del acceso con Google y las tres de administración. Ninguna desborda. Revisadas a ojo en móvil las que llegaron después de la revisión anterior (galería y nutrición del detalle, planificador, lista de la compra, nutrientes de la semana, el campo de la contraseña al cambiar el email).
- **Capturas de la guía:** se rehacen la 08 (mi alimentación, ahora hasta los indicadores; los nutrientes tienen la 24) y la 09 (editar un consumo), anteriores a la nota media en las tarjetas. Las demás siguen enseñando lo que hay: lo que cambió después de tomarlas queda fuera de su encuadre o solo aparece con Google configurado.
- **Dependencias:**
  - npm: ninguna vulnerabilidad en las de producción (`npm audit --omit=dev`). Las 9 «moderadas» son de herramientas de desarrollo (ESLint y typescript-eslint, a través de `minimatch` 10), sin arreglo hasta que ESLint lo publique; Dependabot lo traerá (D-072).
  - Maven: las 131 de runtime, consultadas en OSV.dev. Solo `ognl` 3.3.4, que traía Thymeleaf (GHSA-crwc-vw2r-qr6m: se salta su lista de bloqueo con reflexión, si alguien controla las expresiones). Aquí no era explotable, porque las plantillas de correo son fijas y los datos del usuario entran como variables escapadas, pero sobraba: las plantillas se evalúan ahora con SpEL (`thymeleaf-spring6`, `SpringTemplateEngine`) y OGNL se excluye.
- **Criterios de aceptación (12):**
  - `docker compose --profile full`: se prueba en la integración continua en cada cambio (D-060).
  - Casos de uso y requisitos funcionales de la memoria: cubiertos por las pruebas e2e y la guía.
  - Reglas 6.1–6.9 con tests y cobertura del backend ≥ 80 % en servicios; `mvn verify`, `npm test` y e2e en verde.
  - Lighthouse: lo de arriba.
  - Móvil, tableta y escritorio: lo de arriba.
  - Sin secretos en el repositorio y `.env.example` completo: todas las variables que usa la configuración están en `.env.example` y en la tabla del README.
  - Documentación al día.
- **Pendiente fuera del repositorio:** probar el acceso con Google con unas credenciales reales (D-085) y activar en GitHub las actualizaciones de seguridad de Dependabot.

## D-088 · Confirmación del email
- **Fase:** después del cierre, a propuesta mía tras la revisión de seguridad (D-086), que dejó claro que el punto débil era no comprobar los emails.
- **Decidido por:** usuario, que una cuenta sin confirmar pueda entrar y usarlo todo, con un aviso; yo, el resto.
- **Al registrarse**, el correo de bienvenida lleva un enlace `/confirmar-email?token=…` (uno solo, no dos correos). Vale 24 horas y una vez; del token solo se guarda el SHA-256, como en la recuperación de la contraseña (6.9). Solo hay un enlace válido a la vez: pedir otro anula el anterior.
- **Sin confirmar** se puede usar todo (desde D-089, salvo crear ingredientes). Bajo la cabecera, un aviso lo recuerda y permite pedir el enlace otra vez (`POST /api/usuarios/me/email/confirmacion`, con el límite del olvido de contraseña: envía un correo) o ir a «Mi perfil» para corregir un email mal escrito. Se descartó impedir el login hasta confirmar: quien escribiera mal su email no podría entrar para corregirlo.
- **Cambiar el email** (que ya pedía la contraseña, D-086) no lo cambia todavía: el nuevo espera en `EMAIL_PENDIENTE` y se le envía el enlace. La cuenta sigue con el anterior, que recibe los correos, hasta que se confirma; entonces cambia, se avisa al anterior y el enlace de recuperación pendiente deja de valer. El cambio se puede cancelar. Si al confirmarlo otra cuenta ya usa ese email, 409 y no cambia nada. Cambiar solo las mayúsculas es el mismo email: se guarda sin más.
- **Otras formas de saber que el email es suyo:**
  - las cuentas creadas con Google: Google lo ha verificado;
  - el enlace de recuperación de la contraseña: llega a ese email, así que restablecerla lo confirma;
  - vincular con Google sabiendo la contraseña, si el email de Google es exactamente el de la cuenta.
- **Google (cambia D-086):** si la cuenta tiene el email confirmado y Google da ese mismo email verificado, se vincula sin pedir la contraseña, que era lo que eligió el usuario en D-085. Si no está confirmado, se sigue pidiendo la contraseña. En los dos casos se avisa por correo.
- **Cuentas que ya existían** (migración V18): empiezan sin confirmar, porque no se sabe si su email es suyo. Las de demostración del perfil `dev` están confirmadas; el administrador inicial, no (su email lo da el `.env`).
- **Sesión:** el login y `/api/auth/sesion` dicen si el email está confirmado (`usuario.emailVerificado`), para pintar el aviso sin otra petición. «Mi perfil» lo actualiza al cargar, por si se confirmó en otro dispositivo. Las sesiones guardadas antes no lo traen y no muestran el aviso hasta el siguiente login.
- **Pantalla `/confirmar-email`:** pública, como la de restablecer la contraseña, porque el enlace puede abrirse en otro dispositivo o sin sesión. Si la sesión de este navegador es de esa cuenta, el aviso desaparece.
- **Accesibilidad:** el aviso del email y el contenedor de los toasts son regiones con nombre («Email sin confirmar» y «Mensajes»; este, solo mientras hay alguno). axe marcaba su texto fuera de los landmarks: la prueba de accesibilidad no se había pasado nunca con un toast en pantalla, y ahora el paso del cambio de email pendiente lo hace. «Avisos» no valía como nombre: ya lo tiene la sección de los avisos push de «Mi perfil».

## D-089 · Segunda revisión de seguridad
- **Fase:** después del cierre. Revisión de seguridad por áreas (secretos, código, dependencias, inyecciones, autenticación, infraestructura y configuración de los agentes), sin red y sin abrir el `.env`; aquí, lo que se confirmó en el código y cómo se resolvió. No salió nada crítico ni alto.
- **Decidido por:** usuario, qué se arregla y las cuatro decisiones de comportamiento (el perfil obligatorio, los topes, cerrar sesión y las fotos de las recetas ocultas); yo, cómo.
- **Fotos (cambia D-069):** como mucho 25 megapíxeles (antes 64) y dos imágenes descomprimiéndose a la vez (`ProcesadorImagenes.TURNOS`), en las subidas y en las versiones reducidas. Un PNG casi uniforme de 8000 × 8000 cabe en 5 MB y ocupa unos 256 MB al abrirse: unas pocas subidas simultáneas agotaban la memoria. Una foto real de 5 MB no llega a 25 megapíxeles.
- **Avisos push (D-078):**
  - La clave del navegador (`p256dh`) se comprueba al suscribirse: tiene que ser un punto de la curva P-256. Antes solo se miraban la longitud y el primer byte, y con un punto que no es de la curva el cifrado fallaba en cada aviso.
  - Un fallo con un navegador ya no corta el lote de recordatorios. La excepción salía del bucle de `avisarComidas` con las comidas ya reclamadas, y las siguientes se quedaban sin aviso.
  - La URL de la suscripción se vuelve a comprobar antes de cada envío, por si la lista `app.push.servicios` ha cambiado desde que se guardó.
- **Recetas:** como mucho 50 ingredientes, con el mismo tope en el formulario. Sin él, una petición con miles de ids llegaba a un `IN (…)` enorme. Es un solo `@Size(min = 1, max = 50)`, sin `@NotEmpty`: con los dos, una lista vacía daba dos mensajes y el OpenAPI perdía el mínimo.
- **Sesiones (D-070):**
  - Una cuenta nueva empieza con una versión de sesión al azar (hasta mil millones, para que los incrementos no desborden el `INT`). La baja deja libre el nombre de usuario (D-064), y la cuenta nueva empezaba en 0, como la borrada: un token de esta que aún no hubiera caducado entraba en la nueva.
  - Bloquear a un usuario anula sus sesiones: al desbloquearlo, sus tokens anteriores no vuelven a valer.
  - Confirmar un email nuevo cierra las sesiones de la cuenta. Si el enlace se abre en un navegador con la sesión de esa misma cuenta, la respuesta trae una cookie nueva y ese navegador sigue dentro.
- **Perfil `e2e`:** solo vacía bases de datos cuyo nombre termine en `_test` (la de los tests, en local) o `_e2e` (la de la integración continua). Va dentro del JAR y, activado por error contra la base de datos de desarrollo o de producción, la habría vaciado.
- **Datos de demostración (D-015):** `R__datos_demo.sql` ya no actualiza las cuentas que existan. Como el perfil por defecto es `dev`, un despliegue sin `SPRING_PROFILES_ACTIVE` contra una base de datos real cambiaba el email y la contraseña de un usuario llamado `lucia`, `marcos` o `carmen`.
- **Errores:** una excepción de Spring MVC sin manejador propio conserva su estado 4xx; por ejemplo, 406 sin cuerpo si el `Accept` no admite JSON. Antes era un 500, con la traza en el log.
- **Logs sin emails:** los fallos de envío de correo registran la dirección enmascarada (`l***@toca.local`) y el error con las direcciones también enmascaradas; la traza, que puede repetirlas, solo con el nivel DEBUG. De los errores de restricción de MySQL solo se registra el nombre de la restricción: su mensaje copia el valor que choca, como un email duplicado (`DatosEnLog`).
- **Docker Compose:** MySQL y Mailpit solo escuchan en `127.0.0.1`: Mailpit no pide contraseña y enseña los enlaces de los correos. La aplicación (`--profile full`) corre con el sistema de ficheros de solo lectura, salvo `/tmp` y las fotos, sin capacidades de Linux y con `no-new-privileges`. `.dockerignore` excluye también los `.env` y las carpetas `uploads` de cualquier nivel.
- **Perfil obligatorio:** sin `SPRING_PROFILES_ACTIVE`, la aplicación no arranca (`PerfilObligatorio`, al preparar el entorno, antes de conectarse a la base de datos) y lo explica. Antes el perfil por defecto era `dev`: un despliegue que olvidase la variable quedaba con las cuentas de demostración, Swagger y la cookie sin `Secure`. `mvnw spring-boot:run` sigue arrancando en `dev` (lo fija el `pom.xml`), y los tests, las e2e y Docker ya fijan su perfil.
- **Topes por usuario:** el catálogo de ingredientes es de todos y nada limitaba lo que crea una cuenta gratuita.
  - Crear ingredientes exige el email confirmado (cambia D-088, en la que sin confirmar se podía usar todo) y, como mucho, 30 al día, con las ventanas del límite de intentos (`ingrediente:{usuario}`, D-071) dentro de la transacción del alta: si el alta falla, no cuenta. Responde 403 `EMAIL_SIN_CONFIRMAR` y 429 `LIMITE_INGREDIENTES`. Los administradores no tienen ni una ni otra condición: mantienen el catálogo, y el administrador inicial empieza sin el email confirmado. El formulario lo avisa antes de rellenar nada.
  - Como mucho 500 recetas sin eliminar por autor (se comprueba antes de procesar las fotos), 200 comidas planificadas que aún no han llegado y 10 navegadores con los avisos activados (volver a suscribir uno no cuenta). Responden 409 con su código. Son topes blandos: dos peticiones a la vez pueden pasar uno.
  - Si el servidor rechaza una suscripción a los avisos, «Mi perfil» dice por qué y el navegador deja de estar suscrito: antes parecía activado aunque el servidor no la hubiera guardado.
- **Se dejan como están:**
  - Cerrar sesión solo borra la cookie: el token sigue valiendo hasta que caduca (8 horas). Va en una cookie `HttpOnly` y el JavaScript nunca lo ve, así que haría falta haberlo copiado antes; invalidarlo en el servidor cerraría también los demás dispositivos o necesitaría una lista de tokens revocados.
  - Las fotos de una receta oculta siguen sirviéndose a quien tenga su URL: las imágenes son públicas (D-045) y sus nombres, al azar, no se pueden adivinar.
  - Que quien active los avisos en un navegador se quede con su suscripción, aunque fuera de otra cuenta (D-078): es lo que permite compartir el ordenador, y para quitársela a otra persona habría que conocer la URL de su suscripción, que es secreta.
  - El registro dice si un email o un nombre de usuario ya tienen cuenta (D-086).
- **Dependencias** (con red, con permiso del usuario): `npm audit` y OSV no encuentran nada en lo que va en producción: las 130 dependencias de Maven de runtime y los paquetes de npm del build. Solo avisan de `minimatch` 10.2.6 (GHSA-g3hv-75wg-m249, denegación de servicio con ciertos patrones glob), que llega con ESLint y typescript-eslint: es de desarrollo, solo procesa patrones del propio proyecto y se actualizará con ellas (Dependabot).
- **Integración continua:** el `checkout` no guarda las credenciales (ningún paso usa git con ellas), el secreto del job de Docker se enmascara como el del e2e, y los artefactos se conservan 7 días: las trazas de Playwright llevan cookies de sesión de prueba.
- **Pendiente:** un límite por cuenta en el login (además del de la IP), y fijar por digest las imágenes de Docker y por SHA las acciones de GitHub. **Hecho en D-090.**

## D-090 · Tercera revisión de seguridad
- **Fase:** después de D-089. Revisión por áreas (secretos, código, dependencias, inyecciones, autenticación, infraestructura, configuración de los agentes e instrucciones ocultas), sin red y sin abrir el `.env`; aquí, lo que se confirmó en el código y cómo se resolvió. No salió nada crítico ni alto.
- **Decidido por:** usuario, qué se arregla, que los proxies de confianza sean por defecto solo esta máquina, el coste de bcrypt en los tests, el tope de consumos y el límite suave por cuenta; yo, cómo.
- **Proxies de confianza (cambia D-086):**
  - Tomcat creía `X-Forwarded-For` de cualquier IP de las redes privadas, su valor por defecto. Un cliente que llegara desde una (la LAN o, con Docker Compose, la pasarela de Docker, por la que llegan todos) ponía otra IP en cada petición y estrenaba contador: los límites del login, el registro y el olvido de contraseña no se aplicaban.
  - Ahora `server.tomcat.remoteip.internal-proxies` sale de `PROXIES_DE_CONFIANZA`, por defecto `127.0.0.0/8, ::1/128`: un proxy en la misma máquina sigue funcionando sin configurar nada. Con el proxy en otra hay que poner su rango; si no, la aplicación ve la IP del proxy, los límites se comparten entre todos y no llegan `X-Forwarded-Proto` ni `X-Forwarded-Host` (HSTS, vuelta del acceso con Google). Se prefirió eso a que olvidar la variable dejara los límites sin efecto.
  - En CIDR y no como expresión regular: el `.env` se lee como `.properties`, donde la barra invertida es un carácter de escape, y Docker Compose lo lee a su manera. Con una `/`, Tomcat lo trata como rangos; vacía, no se fía de ninguno. En `.env.example` va comentada, porque vacía anularía el valor por defecto.
  - `ProxiesDeConfianzaTest` lee `application-prod.yml` y comprueba los rangos con `NetMaskSet`, la clase con la que Tomcat los compara.
- **Docker Compose (perfil `full`):**
  - La aplicación escucha solo en `127.0.0.1:8080`, como MySQL y Mailpit desde D-089: sirve por HTTP con `COOKIE_SEGURA=false`, y la cookie de la sesión viajaba en claro por la red.
  - Sin proxy delante, `PROXIES_DE_CONFIANZA` va vacía.
  - Sin `env_file`: el contenedor recibía todo el `.env`, también `MYSQL_ROOT_PASSWORD` y `TEST_DB_URL`. Ahora `environment` lista las variables de la aplicación, y una nueva hay que añadirla allí.
- **Contraseñas (cambia D-024):**
  - Una contraseña nueva de más de 72 bytes daba 500 en el registro, el restablecimiento y el cambio de contraseña: bcrypt se niega a cifrarla, y `@Size(max = 72)` cuenta caracteres. 40 «ñ» son 80 bytes (comprobado con Spring Security 7.1.2). Ahora `@CabeEnBcrypt` la mide en bytes, y en el frontend, el validador `cabeEnBcrypt`. Solo falla con 72 caracteres o menos: con más ya avisa el `@Size`, y no salen dos mensajes. Con un `ADMIN_INITIAL_PASSWORD` de más de 72 bytes, el administrador inicial no se crea y el log dice por qué (antes, el arranque fallaba).
  - bcrypt pasa del coste 10, el de Spring por defecto, al 12 (`CifradoPassword.COSTE`): a quien tenga una copia de la base de datos, cada intento le cuesta cuatro veces más. Comprobar una contraseña pasa de unos 63 a unos 255 ms; lo que la comprueba tiene el límite de intentos.
  - El coste es `app.contrasenas.coste-bcrypt` (`ContrasenasProperties`), 12. El perfil `test` lo baja a 5: con 12, `mvnw verify` pasaba de 1:13 a 2:20, porque muchos tests cifran contraseñas. Fuera de ese perfil, con menos de 12 la aplicación no arranca (`CifradoPassword.coste`): nadie puede debilitar así las contraseñas de producción. 5 y no 4, el mínimo de bcrypt, para que un hash de coste 4 sirva para probar que uno antiguo se rehace.
  - Los hashes anteriores siguen valiendo y se rehacen al entrar, con el mecanismo de Spring Security (`UsuarioDetallesService.updatePassword`). No sube la versión de sesión, porque la contraseña es la misma, y solo se guarda si el hash no ha cambiado mientras tanto, para no deshacer un cambio de contraseña simultáneo; la comparación es binaria, porque la colación de la tabla no distingue mayúsculas. Si no se puede guardar, el login sigue. Por eso el login ya no es de solo lectura.
  - El login responde `CREDENCIALES_INVALIDAS` a una contraseña de más de 72 bytes sin compararla. bcrypt solo mira los 72 primeros, así que entraba la buena seguida de cualquier cosa, y al rehacer un hash antiguo se negaría a cifrarla (500).
- **Topes (amplía D-089):**
  - Como mucho 50 consumos registrados al día por usuario, también los del planificador marcados como comidos: nadie come tantas recetas, y sin tope un script podía llenar la tabla y hacer lentas las cuentas de su semana. Como los ingredientes nuevos, con las ventanas del límite de intentos (`consumo:{usuario}`, D-071) dentro de la transacción del alta, y 429 `LIMITE_CONSUMOS` con `Retry-After`. Las pantallas ya muestran el mensaje de cualquier 4xx.
  - Los textos de búsqueda, como mucho 100 caracteres (`titulo`, `q`, `nombre`), y el autor del filtro de recetas, 30, como un nombre de usuario. El filtro de ingredientes, como mucho 50, los que puede tener una receta. Son `LIKE '%…%'` y un `IN (…)` en cada consulta, y solo los limitaba el tamaño de la URL (8 KB). En el frontend, `maxlength` en esos campos.
- **Descartado tras comprobarlo:** consumir dos veces una comida planificada con dos peticiones a la vez. `PlanService.propia()` ya la bloquea (`SELECT … FOR UPDATE`), y dos comidas en el mismo hueco las impide `UK_USUARIO_RECETA_PLANIFICADA_HUECO`.
- **Cadena de suministro** (con red, con permiso del usuario; pendiente de D-089):
  - Las acciones de la CI, fijadas por el SHA del commit, con la versión en un comentario (`actions/checkout@3d3c42e… # v7.0.1`). Una etiqueta como `v7` se puede mover, y la acción que entrara así leería el código de un repositorio privado. Cada SHA es el del commit al que apunta la etiqueta mayor en el repositorio oficial de la acción, y Dependabot (`github-actions`) los actualiza con su comentario.
  - Las imágenes, fijadas por el digest del índice multiarquitectura: las tres del `Dockerfile`, la de su `# syntax=`, y MySQL y Mailpit en `docker-compose.yml`. Los parches ya no llegan solos en cada build (D-072): Dependabot propone cada semana el digest nuevo de la misma etiqueta, también para el `Dockerfile` (`docker`, sin versiones mayores de Node ni de Java). Si no actualiza el `# syntax=`, se sube a mano.
  - El Maven Wrapper lleva el SHA-256 del zip (`distributionSha256Sum`), comprobado con el `.sha512` del repositorio de Maven y de los dos servidores de Apache. `mvnw` y `mvnw.cmd` no ejecutan un Maven descargado que no coincida; comprobado con un hash erróneo. Ojo: sin `unzip`, `mvnw` descarga el `.tar.gz`, que tiene otro SHA-256, y no arranca. Así falló la primera vez el build de la imagen Docker, porque `eclipse-temurin` no lo trae: su etapa de build lo instala, y no llega a la imagen final.
  - Se deja como está la descarga de Node del perfil `frontend`: `frontend-maven-plugin` no admite un hash, y el perfil es opcional. El JAR de la imagen Docker usa la de `node:24-alpine`, fijada por digest.
- **Límite por cuenta en el login** (pendiente de D-089):
  - El de la IP no frena a quien prueba contraseñas contra una cuenta desde muchas IPs. Ahora, tras 20 contraseñas fallidas en 15 minutos con un mismo usuario o email, desde las IPs que sea, el login responde 429 `DEMASIADOS_INTENTOS` sin comprobar la contraseña, aunque llegue la buena (`app.limite-intentos.login-cuenta`).
  - Es suave porque cualquiera puede agotarlo: los nombres de usuario son públicos (el autor de cada receta). Solo cuentan los fallos. Restablecer la contraseña con el enlace del correo lo pone a cero, y el acceso con Google no se ve afectado. El mensaje, que sugiere el enlace, es el mismo exista o no la cuenta.
  - El contador va por lo escrito, no por la cuenta: si el usuario y su email compartieran contador, agotándolo con el nombre, que es público, se sabría qué email es de ese usuario. A cambio, quien ataque tiene el doble de intentos (20 por el usuario y 20 por el email). La colación de la tabla ya junta las mayúsculas y los acentos.
  - El fallo se cuenta en su propia transacción (`LimiteIntentosService.registrarFallo`, `REQUIRES_NEW`): el login que falla deshace la suya. Comprobar sin contar es `agotado`.
  - En los tests, 5 fallos: es de cada cuenta, y cada test usa las suyas.
- **Pendiente de esta revisión:** reglas `deny` locales para los agentes, solo si el usuario lo pide.

## D-091 · Contorno de los campos de formulario
- **Fase:** después del cierre, a raíz de un comentario del usuario: en el login no se distinguía dónde había que escribir.
- **Problema:** los campos, los desplegables, las casillas y los interruptores llevaban el borde gris de Bootstrap (`#dee2e6`), con un contraste de 1,3:1 sobre la tarjeta blanca y de 1,2:1 sobre el fondo de la página. WCAG 1.4.11 pide 3:1 en lo que hace falta para reconocer un control, y un campo vacío no tiene otra pista que su borde. Ni axe (D-075) ni Lighthouse (D-059) lo detectan: no miden el contraste de lo que no es texto.
- **Decisión:**
  - Un marrón grisáceo de la gama de la marca, `#8f7f70` (`$borde-campo` en `styles.scss`, y `--toca-borde-campo` para lo que no es de Bootstrap, como el editor de las instrucciones). Da 3,9:1 sobre blanco, 3,4:1 sobre el fondo de la página y 3,1:1 sobre el beige claro. Se aplica a los campos, los desplegables, los grupos con botón (la contraseña con el ojo), las casillas, las opciones y el círculo del interruptor apagado.
  - Con el foco, el borde pasa al verde de la marca (`$primary`, 5,1:1). El de Bootstrap, un verde pálido a medio camino del blanco, quedaría más claro que el borde normal, y el campo con el foco parecería perderlo.
  - Los bordes decorativos (tarjetas, tablas, separadores) siguen con el gris de Bootstrap: no delimitan nada que haya que usar.
- **Pendiente:** las capturas de `docs/img/guia/` se hicieron con el borde anterior.
