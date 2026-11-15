package es.alimentarse.alimentacion;

import static org.assertj.core.api.Assertions.assertThat;

import es.alimentarse.support.DatosDePrueba;
import es.alimentarse.support.TestIntegracion;
import es.alimentarse.support.TokensDePrueba;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

@TestIntegracion
@Transactional
class AlimentacionControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    TokensDePrueba tokens;

    @Autowired
    DatosDePrueba datos;

    /**
     * Reproduce el ejemplo de la sección 6.4 (los datos demo de lucia): en los últimos 7 días, pollo con tallarines
     * ×3, boquerones ×1 y batido ×2; los rollos de canela, hace 9 días, quedan fuera.
     */
    private void crearHistorialDelEjemplo(String usuario) {
        datos.usuario("autor");
        datos.usuario(usuario);
        int pollo = datos.receta("Pollo con tallarines", "autor", true, 30, "Pollo", "Tallarines", "Cebolla");
        int boquerones = datos.receta("Boquerones", "autor", true, 30, "Boquerón", "Limón", "Sal");
        int batido = datos.receta("Batido", "autor", true, 30, "Plátano", "Fresa", "Leche semidesnatada");
        int rollos = datos.receta("Rollos", "autor", true, 30, "Harina", "Chocolate blanco");
        datos.consumo(usuario, batido, Duration.ofHours(10));
        datos.consumo(usuario, pollo, Duration.ofHours(26));
        datos.consumo(usuario, boquerones, Duration.ofHours(50));
        datos.consumo(usuario, pollo, Duration.ofHours(74));
        datos.consumo(usuario, batido, Duration.ofHours(98));
        datos.consumo(usuario, pollo, Duration.ofHours(122));
        datos.consumo(usuario, rollos, Duration.ofHours(218));
    }

    @Test
    void losIndicadoresReproducenElEjemploDeLaEspecificacion() {
        crearHistorialDelEjemplo("comensal");

        assertThat(resumen("comensal"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying(
                        "$.grupos7Dias[*].grupo.nombre",
                        v -> v.assertThat()
                                .asArray()
                                .containsExactly(
                                        "Carne",
                                        "Pasta",
                                        "Pescado",
                                        "Leches y derivados",
                                        "Esenciales",
                                        "Fruta",
                                        "Hortalizas"))
                .hasPathSatisfying(
                        "$.grupos7Dias[0]",
                        v -> v.assertThat()
                                .asMap()
                                .containsEntry("consumidas", 3)
                                .containsEntry("limite", 3)
                                .containsEntry("porcentaje", 100)
                                .containsEntry("color", "ROJO"))
                .hasPathSatisfying(
                        "$.grupos7Dias[1]",
                        v -> v.assertThat()
                                .asMap()
                                .containsEntry("consumidas", 3)
                                .containsEntry("limite", 4)
                                .containsEntry("porcentaje", 75)
                                .containsEntry("color", "AMARILLO"))
                .hasPathSatisfying(
                        "$.grupos7Dias[2]",
                        v -> v.assertThat()
                                .asMap()
                                .containsEntry("consumidas", 1)
                                .containsEntry("porcentaje", 25)
                                .containsEntry("color", "VERDE"))
                .hasPathSatisfying(
                        "$.grupos7Dias[5]",
                        v -> v.assertThat()
                                .asMap()
                                .containsEntry("consumidas", 3)
                                .containsEntry("limite", null)
                                .containsEntry("porcentaje", 100)
                                .containsEntry("color", "VERDE"));
    }

    @Test
    void ultimasMasConsumidasEIngredientesMasUsados() {
        crearHistorialDelEjemplo("comensal");

        assertThat(resumen("comensal"))
                .bodyJson()
                .hasPathSatisfying(
                        "$.ultimas[*].receta.titulo",
                        v -> v.assertThat().asArray().containsExactly("Batido", "Pollo con tallarines", "Boquerones"))
                // Boquerones y Rollos empatan a 1: va primero el consumido más recientemente
                .hasPathSatisfying(
                        "$.masConsumidas[*].receta.titulo",
                        v -> v.assertThat().asArray().containsExactly("Pollo con tallarines", "Batido", "Boquerones"))
                .hasPathSatisfying(
                        "$.masConsumidas[*].veces",
                        v -> v.assertThat().asArray().containsExactly(3, 2, 1))
                .hasPathSatisfying(
                        "$.ingredientesMasUsados[*].ingrediente.nombre",
                        v -> v.assertThat().asArray().containsExactly("Cebolla", "Pollo", "Tallarines", "Fresa"))
                .hasPathSatisfying(
                        "$.ingredientesMasUsados[*].veces",
                        v -> v.assertThat().asArray().containsExactly(3, 3, 3, 2))
                .hasPathSatisfying(
                        "$.ingredientesMasUsados[0].ingrediente.grupo.nombre",
                        v -> v.assertThat().isEqualTo("Hortalizas"));
    }

    @Test
    void losIngredientesMasUsadosNoIncluyenLosGruposBasicos() {
        datos.usuario("autor");
        datos.usuario("comensal");
        int guiso = datos.receta(
                "Guiso", "autor", true, 30, "Sal", "Aceite de oliva", "Agua", "Pimentón", "Comino", "Garbanzos");
        int ensalada = datos.receta("Ensalada", "autor", true, 30, "Sal", "Aceite de oliva", "Vinagre", "Tomate");
        for (int i = 0; i < 3; i++) {
            datos.consumo("comensal", guiso, Duration.ofHours(10 + i));
            datos.consumo("comensal", ensalada, Duration.ofHours(20 + i));
        }

        // Sal y aceite (Esenciales) salen 6 veces y el pimentón (Especias) 3, pero no se muestran
        assertThat(resumen("comensal"))
                .bodyJson()
                .hasPathSatisfying(
                        "$.ingredientesMasUsados[*].ingrediente.nombre",
                        v -> v.assertThat().asArray().containsExactly("Garbanzos", "Tomate"))
                .hasPathSatisfying(
                        "$.ingredientesMasUsados[*].veces",
                        v -> v.assertThat().asArray().containsExactly(3, 3));
    }

    @Test
    void lasRecetasEmpatadasEnVecesAparecenTodasPorConsumoMasReciente() {
        datos.usuario("autor");
        datos.usuario("comensal");
        int sopa = datos.receta("Sopa", "autor", true, 30, "Puerro");
        int ensalada = datos.receta("Ensalada", "autor", true, 30, "Lechuga");
        int tostada = datos.receta("Tostada", "autor", true, 30, "Tomate");
        datos.consumo("comensal", sopa, Duration.ofDays(3));
        datos.consumo("comensal", ensalada, Duration.ofDays(1));
        datos.consumo("comensal", tostada, Duration.ofDays(2));

        assertThat(resumen("comensal"))
                .bodyJson()
                .hasPathSatisfying(
                        "$.masConsumidas[*].receta.titulo",
                        v -> v.assertThat().asArray().containsExactly("Ensalada", "Tostada", "Sopa"))
                .hasPathSatisfying(
                        "$.masConsumidas[*].veces",
                        v -> v.assertThat().asArray().containsExactly(1, 1, 1));
    }

    @Test
    void laVentanaDeSieteDiasEnLaBaseDeDatos() {
        datos.usuario("autor");
        datos.usuario("dentro");
        datos.usuario("fuera");
        int tarta = datos.receta("Tarta", "autor", true, 30, "Chocolate negro");
        datos.consumo("dentro", tarta, Duration.ofDays(7).minusMinutes(1));
        datos.consumo("fuera", tarta, Duration.ofDays(7).plusMinutes(1));

        assertThat(resumen("dentro"))
                .bodyJson()
                .hasPathSatisfying(
                        "$.grupos7Dias[0].grupo.nombre", v -> v.assertThat().isEqualTo("Dulces"))
                .hasPathSatisfying("$.grupos7Dias[0].color", v -> v.assertThat().isEqualTo("ROJO"));
        assertThat(resumen("fuera"))
                .bodyJson()
                .hasPathSatisfying(
                        "$.grupos7Dias", v -> v.assertThat().asArray().isEmpty())
                .hasPathSatisfying(
                        "$.masConsumidas", v -> v.assertThat().asArray().hasSize(1));
    }

    @Test
    void sinConsumosElResumenEstaVacio() {
        datos.usuario("nuevo");

        assertThat(resumen("nuevo"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.ultimas", v -> v.assertThat().asArray().isEmpty())
                .hasPathSatisfying(
                        "$.masConsumidas", v -> v.assertThat().asArray().isEmpty())
                .hasPathSatisfying(
                        "$.ingredientesMasUsados", v -> v.assertThat().asArray().isEmpty())
                .hasPathSatisfying(
                        "$.grupos7Dias", v -> v.assertThat().asArray().isEmpty());
    }

    @Test
    void elResumenExigeSesion() {
        assertThat(mvc.get().uri("/api/alimentacion/resumen")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private MvcTestResult resumen(String usuario) {
        return mvc.get()
                .uri("/api/alimentacion/resumen")
                .header(HttpHeaders.AUTHORIZATION, tokens.usuario(usuario))
                .exchange();
    }
}
