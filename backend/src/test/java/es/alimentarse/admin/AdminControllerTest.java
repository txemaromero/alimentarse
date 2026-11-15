package es.alimentarse.admin;

import static org.assertj.core.api.Assertions.assertThat;

import es.alimentarse.support.DatosDePrueba;
import es.alimentarse.support.TestIntegracion;
import es.alimentarse.support.TokensDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

@TestIntegracion
@Transactional
class AdminControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    TokensDePrueba tokens;

    @Autowired
    DatosDePrueba datos;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void crearUsuarios() {
        datos.admin("gestora");
        datos.admin("gestor2");
        datos.usuario("gestion-lucia");
        jdbc.update(
                "UPDATE usuarios SET NOMBRE = 'Lucía', APELLIDO1 = 'Peñalver', EMAIL = 'lucia.gestion@pruebas.local'"
                        + " WHERE ID = 'gestion-lucia'");
    }

    @Test
    void laBusquedaEncuentraPorIdNombreSinTildeYEmailConLaMarcaDeAdministrador() {
        assertThat(listar("q=GESTOR", "gestora"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying(
                        "$.contenido[*].id", v -> v.assertThat().asArray().containsExactly("gestor2", "gestora"))
                .hasPathSatisfying(
                        "$.contenido[*].admin", v -> v.assertThat().asArray().containsExactly(true, true));
        assertThat(listar("q=lucia penalver", "gestora"))
                .bodyJson()
                .hasPathSatisfying("$.totalElementos", v -> v.assertThat().isEqualTo(0));
        assertThat(listar("q=penalver", "gestora"))
                .bodyJson()
                .hasPathSatisfying("$.contenido[0].id", v -> v.assertThat().isEqualTo("gestion-lucia"))
                .hasPathSatisfying("$.contenido[0].admin", v -> v.assertThat().isEqualTo(false));
        assertThat(listar("q=lucia.gestion@", "gestora"))
                .bodyJson()
                .hasPathSatisfying(
                        "$.contenido[*].id", v -> v.assertThat().asArray().containsExactly("gestion-lucia"));
        assertThat(listar("q=gestor&size=1&page=1", "gestora"))
                .bodyJson()
                .hasPathSatisfying(
                        "$.contenido[*].id", v -> v.assertThat().asArray().containsExactly("gestora"))
                .hasPathSatisfying("$.totalPaginas", v -> v.assertThat().isEqualTo(2));
    }

    @Test
    void soloLosAdministradoresGestionanUsuarios() {
        assertThat(mvc.get()
                        .uri("/api/admin/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("gestion-lucia")))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri("/api/admin/usuarios")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void darYQuitarElRolDeAdministrador() {
        assertThat(cambiar("gestion-lucia", "true", "gestora"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.admin", v -> v.assertThat().isEqualTo(true));
        assertThat(roles("gestion-lucia")).isEqualTo(2);

        assertThat(cambiar("gestion-lucia", "false", "gestora"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.admin", v -> v.assertThat().isEqualTo(false));
        assertThat(roles("gestion-lucia")).isEqualTo(1);
    }

    @Test
    void unAdministradorNoPuedeQuitarseElRolASiMismo() {
        assertThat(cambiar("gestora", "false", "gestora"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("PROPIO_ROL"))
                .hasPathSatisfying("$.mensaje", v -> v.assertThat().isEqualTo(AdminService.PROPIO_ROL));
    }

    @Test
    void unAdministradorDegradadoNoPuedeSeguirAdministrandoConSuTokenAntiguo() {
        assertThat(cambiar("gestor2", "false", "gestora")).hasStatusOk();

        // Su token lleva ADMIN, pero al cambiarle el rol se anuló su sesión (D-070)
        assertThat(cambiar("gestora", "false", "gestor2"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("NO_AUTENTICADO"));
        assertThat(roles("gestora")).isEqualTo(2);
    }

    @Test
    void unUsuarioInexistenteOUnCuerpoIncompletoDanError() {
        assertThat(cambiar("no-existe", "true", "gestora")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.put()
                        .uri("/api/admin/usuarios/gestion-lucia/admin")
                        .header(HttpHeaders.AUTHORIZATION, tokens.admin("gestora"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying(
                        "$.errores.admin", v -> v.assertThat().asString().isNotBlank());
    }

    private MvcTestResult listar(String consulta, String admin) {
        return mvc.get()
                .uri("/api/admin/usuarios?" + consulta)
                .header(HttpHeaders.AUTHORIZATION, tokens.admin(admin))
                .exchange();
    }

    private MvcTestResult cambiar(String usuario, String admin, String solicitante) {
        return mvc.put()
                .uri("/api/admin/usuarios/" + usuario + "/admin")
                .header(HttpHeaders.AUTHORIZATION, tokens.admin(solicitante))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"admin\": " + admin + "}")
                .exchange();
    }

    private int roles(String usuario) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM usuario_rol WHERE USUARIO = ?", Integer.class, usuario);
    }
}
