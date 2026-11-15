package es.alimentarse.cuenta;

import static org.assertj.core.api.Assertions.assertThat;

import es.alimentarse.support.DatosDePrueba;
import es.alimentarse.support.TestIntegracion;
import es.alimentarse.support.TokensDePrueba;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

/** Descargar los datos personales y darse de baja (D-064), contra MySQL real. */
@TestIntegracion
@Transactional
class CuentaControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    DatosDePrueba datos;

    @Autowired
    TokensDePrueba tokens;

    @Autowired
    PasswordEncoder passwordEncoder;

    private int consumida;
    private int sinConsumir;
    private int ajena;

    @BeforeEach
    void crearFixture() {
        datos.usuario("saliente");
        datos.usuario("vecina");
        jdbc.update("UPDATE usuarios SET PASSWORD = ? WHERE ID = 'saliente'", passwordEncoder.encode("demo1234"));
        consumida = datos.receta("Tortilla de la saliente", "saliente", true, 5, "Huevos", "Patata");
        sinConsumir = datos.receta("Borrador de la saliente", "saliente", false, 3, "Lentejas");
        ajena = datos.receta("Lentejas de la vecina", "vecina", true, 4, "Lentejas");
        // Galería de la receta consumida (D-083)
        jdbc.update("UPDATE recetas SET IMAGEN = 'principal-saliente.jpg' WHERE ID = ?", consumida);
        jdbc.update(
                "INSERT INTO foto_receta (RECETA, NOMBRE, ORDEN) VALUES (?, 'principal-saliente.jpg', 0),"
                        + " (?, 'segunda-saliente.jpg', 1)",
                consumida,
                consumida);
        datos.consumo("vecina", consumida, Duration.ofDays(1));
        datos.consumo("saliente", ajena, Duration.ofDays(2));
        datos.like("saliente", ajena);
        datos.like("vecina", consumida);
        datos.comidaPlanificada("saliente", ajena, Duration.ofDays(1));
        jdbc.update(
                "INSERT INTO valoracion_receta (USUARIO, RECETA, NOTA, COMENTARIO, FECHA)"
                        + " VALUES ('saliente', ?, 4, 'Muy buenas', NOW())",
                ajena);
        jdbc.update("INSERT INTO suscripcion_push (USUARIO, ENDPOINT, ENDPOINT_HASH, P256DH, AUTH, ZONA, FECHA)"
                + " VALUES ('saliente', 'https://fcm.googleapis.com/fcm/send/saliente', REPEAT('a', 64), 'p', 'a',"
                + " 'Europe/Madrid', NOW())");
    }

    @Test
    void laDescargaTraeTodosSusDatosSinLaContrasena() {
        MvcTestResult respuesta = mvc.get()
                .uri("/api/usuarios/me/datos")
                .header(HttpHeaders.AUTHORIZATION, tokens.usuario("saliente"))
                .exchange();

        assertThat(respuesta)
                .hasStatusOk()
                .hasHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"alimentarse-saliente.json\"")
                .hasHeader(HttpHeaders.CACHE_CONTROL, "no-store")
                .bodyJson()
                .hasPathSatisfying("$.perfil.id", v -> v.assertThat().isEqualTo("saliente"))
                .hasPathSatisfying("$.perfil.email", v -> v.assertThat().isEqualTo("saliente@pruebas.local"))
                .hasPathSatisfying(
                        "$.recetas[*].titulo",
                        v -> v.assertThat()
                                .asArray()
                                .containsExactly("Tortilla de la saliente", "Borrador de la saliente"))
                .hasPathSatisfying(
                        "$.recetas[0].ingredientes[*].nombre",
                        v -> v.assertThat().asArray().containsExactly("Huevos", "Patata"))
                .hasPathSatisfying(
                        "$.recetas[0].ingredientes[0].grupo",
                        v -> v.assertThat().isEqualTo("Huevos"))
                .hasPathSatisfying(
                        "$.recetas[0].fotos",
                        v -> v.assertThat()
                                .asArray()
                                .containsExactly(
                                        "/api/imagenes/principal-saliente.jpg", "/api/imagenes/segunda-saliente.jpg"))
                .hasPathSatisfying(
                        "$.recetas[1].fotos", v -> v.assertThat().asArray().isEmpty())
                .hasPathSatisfying("$.consumos[0].titulo", v -> v.assertThat().isEqualTo("Lentejas de la vecina"))
                .hasPathSatisfying("$.meGusta[0].receta", v -> v.assertThat().isEqualTo(ajena))
                .hasPathSatisfying(
                        "$.comidasPlanificadas[0].receta", v -> v.assertThat().isEqualTo(ajena))
                .hasPathSatisfying("$.valoraciones[0].nota", v -> v.assertThat().isEqualTo(4))
                .hasPathSatisfying(
                        "$.valoraciones[0].comentario", v -> v.assertThat().isEqualTo("Muy buenas"))
                .hasPathSatisfying("$.avisos[0].servicio", v -> v.assertThat().isEqualTo("fcm.googleapis.com"));
        assertThat(respuesta.getResponse().getContentAsByteArray()).isNotEmpty();
        assertThat(contenido(respuesta)).doesNotContain("password", "PASSWORD", "token");
    }

    @Test
    void laBajaBorraLaCuentaYConservaSinAutorLasRecetasQueOtrosHanComido() {
        String sesion = tokens.usuario("saliente");

        assertThat(baja(sesion, "demo1234")).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(contar("SELECT COUNT(*) FROM usuarios WHERE ID = 'saliente'"))
                .isZero();
        assertThat(contar("SELECT COUNT(*) FROM usuario_rol WHERE USUARIO = 'saliente'"))
                .isZero();
        assertThat(contar("SELECT COUNT(*) FROM usuario_receta_consumida WHERE USUARIO = 'saliente'"))
                .isZero();
        assertThat(contar("SELECT COUNT(*) FROM usuario_receta_planificada WHERE USUARIO = 'saliente'"))
                .isZero();
        assertThat(contar("SELECT COUNT(*) FROM like_receta WHERE USUARIO = 'saliente'"))
                .isZero();
        assertThat(contar("SELECT COUNT(*) FROM valoracion_receta WHERE USUARIO = 'saliente'"))
                .isZero();
        assertThat(contar("SELECT COUNT(*) FROM suscripcion_push WHERE USUARIO = 'saliente'"))
                .isZero();
        assertThat(contar("SELECT COUNT(*) FROM recetas WHERE ID = " + sinConsumir))
                .isZero();
        Map<String, Object> conservada = jdbc.queryForMap(
                "SELECT USUARIO, ELIMINADA, PUBLICADA, INSTRUCCIONES, TITULO FROM recetas WHERE ID = ?", consumida);
        assertThat(conservada)
                .containsEntry("ELIMINADA", true)
                .containsEntry("PUBLICADA", false)
                .containsEntry("INSTRUCCIONES", "")
                .containsEntry("TITULO", "Tortilla de la saliente");
        assertThat(conservada.get("USUARIO")).isNull();
        // La que se conserva se queda sin fotos (D-083)
        assertThat(contar("SELECT COUNT(*) FROM foto_receta WHERE RECETA = " + consumida))
                .isZero();
        assertThat(contar("SELECT COUNT(*) FROM like_receta WHERE RECETA = " + consumida))
                .isZero();

        // La vecina conserva su consumo, sin autor, y le sigue contando en los grupos de la semana
        assertThat(get("/api/consumos", tokens.usuario("vecina")))
                .bodyJson()
                .hasPathSatisfying(
                        "$.contenido[0].receta.eliminada", v -> v.assertThat().isEqualTo(true))
                .hasPathSatisfying(
                        "$.contenido[0].receta.autor", v -> v.assertThat().isNull());
        assertThat(get("/api/alimentacion/resumen", tokens.usuario("vecina")))
                .bodyJson()
                .hasPathSatisfying(
                        "$.grupos7Dias[?(@.grupo.nombre == 'Huevos')].consumidas",
                        v -> v.assertThat().asArray().containsExactly(1));

        // La sesión abierta ya no vale, y el nombre de usuario queda libre
        assertThat(get("/api/usuarios/me", sesion)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.post()
                        .uri("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": \"saliente\", \"nombre\": \"Otra\", \"apellido1\": \"Persona\","
                                + " \"email\": \"otra@pruebas.local\", \"password\": \"demo12345\"}"))
                .hasStatus(HttpStatus.CREATED);

        // La cuenta nueva con ese nombre no acepta los tokens de la anterior, aunque no hayan caducado (D-089)
        assertThat(get("/api/usuarios/me", sesion)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void conUnaContrasenaIncorrectaNoSeBorraNada() {
        assertThat(baja(tokens.usuario("saliente"), "incorrecta"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errores.password", v -> v.assertThat().isEqualTo("La contraseña no es correcta"));
        assertThat(baja(tokens.usuario("saliente"), " "))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("VALIDACION"));
        assertThat(contar("SELECT COUNT(*) FROM usuarios WHERE ID = 'saliente'"))
                .isEqualTo(1);
    }

    @Test
    void elUltimoAdministradorNoPuedeDarseDeBaja() {
        jdbc.update("DELETE FROM usuario_rol WHERE ROL = (SELECT ID FROM roles WHERE NOMBRE = 'ADMIN')");
        jdbc.update("INSERT INTO usuario_rol (USUARIO, ROL) SELECT 'saliente', ID FROM roles WHERE NOMBRE = 'ADMIN'");

        assertThat(baja(tokens.admin("saliente"), "demo1234"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("ULTIMO_ADMINISTRADOR"));
    }

    @Test
    void ambasExigenSesion() {
        assertThat(mvc.get().uri("/api/usuarios/me/datos")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.post()
                        .uri("/api/usuarios/me/baja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"x\"}"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private MvcTestResult baja(String token, String password) {
        return mvc.post()
                .uri("/api/usuarios/me/baja")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\": \"" + password + "\"}")
                .exchange();
    }

    private MvcTestResult get(String uri, String token) {
        return mvc.get().uri(uri).header(HttpHeaders.AUTHORIZATION, token).exchange();
    }

    private int contar(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    private static String contenido(MvcTestResult respuesta) {
        try {
            return respuesta.getResponse().getContentAsString();
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
