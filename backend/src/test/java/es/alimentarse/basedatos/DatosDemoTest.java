package es.alimentarse.basedatos;

import static org.assertj.core.api.Assertions.assertThat;

import es.alimentarse.support.BaseDatosTestConfig;
import es.alimentarse.support.TestIntegracion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Datos de demostración del perfil dev (db/dev): se migran sobre la base de datos de test y al final se deja como
 * estaba, sin ellos.
 */
@TestIntegracion
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatosDemoTest {

    private static final String MIGRACIONES = "classpath:db/migration";
    private static final String DEMO = "classpath:db/dev";

    /** Número de ingredientes de cada receta demo: detecta nombres mal escritos que el JOIN descartaría. */
    private static final Map<Integer, Integer> INGREDIENTES_POR_RECETA =
            Map.of(1, 7, 2, 8, 3, 5, 4, 6, 5, 9, 6, 10, 7, 6, 8, 12, 9, 8, 10, 4);

    private static final int CONSUMOS_DEMO = 15;

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeAll
    void migrarConDatosDemo() {
        flyway(MIGRACIONES, DEMO).clean();
        flyway(MIGRACIONES, DEMO).migrate();
    }

    @AfterAll
    void restaurarSinDatosDemo() {
        flyway(MIGRACIONES).clean();
        flyway(MIGRACIONES).migrate();
    }

    @Test
    void lasTresCuentasDemoTienenRolUserYContrasenaDemo1234() {
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        List<String> passwords = jdbc.queryForList(
                "SELECT PASSWORD FROM usuarios WHERE ID IN ('lucia', 'marcos', 'carmen')", String.class);

        assertThat(passwords)
                .hasSize(3)
                .allSatisfy(hash -> assertThat(bcrypt.matches("demo1234", hash)).isTrue());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM usuario_rol ur JOIN roles r ON r.ID = ur.ROL"
                                + " WHERE r.NOMBRE = 'USER' AND ur.USUARIO IN ('lucia', 'marcos', 'carmen')",
                        Integer.class))
                .isEqualTo(3);
    }

    @Test
    void lasDiezRecetasTienenTodosSusIngredientes() {
        assertThat(ingredientesPorReceta()).isEqualTo(INGREDIENTES_POR_RECETA);
        assertThat(jdbc.queryForList("SELECT ID FROM recetas WHERE PUBLICADA = FALSE", Integer.class))
                .containsExactly(2);
    }

    @Test
    void cadaRecetaTieneSuFotoPrincipalYLaTortillaDosMas() {
        // La principal de la galería es la imagen de la receta (D-083)
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM recetas r WHERE r.ID BETWEEN 1 AND 10 AND NOT EXISTS ("
                                + "SELECT 1 FROM foto_receta f WHERE f.RECETA = r.ID AND f.ORDEN = 0"
                                + " AND f.NOMBRE = r.IMAGEN)",
                        Integer.class))
                .isZero();
        assertThat(jdbc.queryForList("SELECT NOMBRE FROM foto_receta WHERE RECETA = 1 ORDER BY ORDEN", String.class))
                .containsExactly("demo-tortilla.jpg", "demo-tortilla-corte.jpg", "demo-tortilla-ingredientes.jpg");
    }

    @Test
    void losConsumosEstanRepartidosEnLosUltimosDiezDias() {
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM usuario_receta_consumida"
                                + " WHERE FECHA BETWEEN NOW() - INTERVAL 10 DAY AND NOW()",
                        Integer.class))
                .isEqualTo(CONSUMOS_DEMO);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuario_receta_consumida", Integer.class))
                .isEqualTo(CONSUMOS_DEMO);
    }

    @Test
    void lasRacionesDeLuciaReproducenElEjemploDeLaEspecificacion() {
        Map<String, Integer> raciones = racionesUltimos7Dias("lucia");

        assertThat(raciones)
                .containsEntry("Carne", 3)
                .containsEntry("Pasta", 3)
                .containsEntry("Pescado", 1)
                .containsEntry("Fruta", 3)
                .doesNotContainKey("Dulces");
    }

    @Test
    void marcosTieneAgotadoElGrupoDulces() {
        assertThat(racionesUltimos7Dias("marcos")).containsEntry("Dulces", 1);
    }

    @Test
    void volverACargarLosDatosDemoNoDuplicaNada() {
        Map<String, Object> recetaAntes = jdbc.queryForMap("SELECT TITULO, FECHA_CREACION FROM recetas WHERE ID = 1");

        new ResourceDatabasePopulator(new ClassPathResource("db/dev/R__datos_demo.sql")).execute(dataSource);
        flyway(MIGRACIONES, DEMO).migrate();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class))
                .isEqualTo(3);
        assertThat(ingredientesPorReceta()).isEqualTo(INGREDIENTES_POR_RECETA);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM like_receta", Integer.class))
                .isEqualTo(11);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM valoracion_receta", Integer.class))
                .isEqualTo(14);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuario_receta_consumida", Integer.class))
                .isEqualTo(CONSUMOS_DEMO);
        assertThat(jdbc.queryForMap("SELECT TITULO, FECHA_CREACION FROM recetas WHERE ID = 1"))
                .isEqualTo(recetaAntes);
    }

    /** Raciones por grupo en los últimos 7 días, según la regla 6.2: cada consumo cuenta una vez por grupo. */
    private Map<String, Integer> racionesUltimos7Dias(String usuario) {
        Map<String, Integer> raciones = new HashMap<>();
        jdbc.query(
                "SELECT g.NOMBRE, COUNT(DISTINCT c.ID) AS RACIONES FROM usuario_receta_consumida c"
                        + " JOIN ingrediente_receta ir ON ir.RECETA = c.RECETA"
                        + " JOIN ingredientes i ON i.ID = ir.INGREDIENTE"
                        + " JOIN grupo_ingrediente g ON g.ID = i.GRUPO"
                        + " WHERE c.USUARIO = ? AND c.FECHA BETWEEN NOW() - INTERVAL 7 DAY AND NOW()"
                        + " GROUP BY g.NOMBRE",
                fila -> {
                    raciones.put(fila.getString("NOMBRE"), fila.getInt("RACIONES"));
                },
                usuario);
        return raciones;
    }

    private Map<Integer, Integer> ingredientesPorReceta() {
        Map<Integer, Integer> recuento = new HashMap<>();
        jdbc.query("SELECT RECETA, COUNT(*) AS N FROM ingrediente_receta GROUP BY RECETA", fila -> {
            recuento.put(fila.getInt("RECETA"), fila.getInt("N"));
        });
        return recuento;
    }

    private Flyway flyway(String... ubicaciones) {
        BaseDatosTestConfig.exigirBaseDeDatosDeTest(dataSource);
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(ubicaciones)
                .cleanDisabled(false)
                .load();
    }
}
