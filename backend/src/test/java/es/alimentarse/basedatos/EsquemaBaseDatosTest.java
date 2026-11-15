package es.alimentarse.basedatos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.alimentarse.support.TestIntegracion;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Esquema de la sección 5: tablas, colación y restricciones. Cada test se deshace al terminar. */
@TestIntegracion
@Transactional
class EsquemaBaseDatosTest {

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void crearAutor() {
        jdbc.update("INSERT INTO usuarios (ID, NOMBRE, APELLIDO1, EMAIL, PASSWORD)"
                + " VALUES ('autor', 'Autor', 'Prueba', 'autor@prueba.local', 'x')");
    }

    @Test
    void seHanAplicadoLasMigracionesVersionadas() {
        List<String> versiones = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = 1 AND version IS NOT NULL"
                        + " ORDER BY installed_rank",
                String.class);

        assertThat(versiones)
                .containsExactly(
                        "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17",
                        "18");
    }

    @Test
    void existenLasTablasDeLaEspecificacionSinDistinguirMayusculasNiAcentos() {
        Map<String, String> colacionPorTabla = jdbc
                .queryForList("SELECT TABLE_NAME, TABLE_COLLATION FROM information_schema.TABLES"
                        + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME <> 'flyway_schema_history'")
                .stream()
                .collect(Collectors.toMap(fila -> ((String) fila.get("TABLE_NAME")).toLowerCase(), fila ->
                        (String) fila.get("TABLE_COLLATION")));

        assertThat(colacionPorTabla)
                .containsOnlyKeys(
                        "roles",
                        "usuarios",
                        "usuario_rol",
                        "grupo_ingrediente",
                        "ingredientes",
                        "tipo_receta",
                        "recetas",
                        "ingrediente_receta",
                        "like_receta",
                        "usuario_receta_consumida",
                        "usuario_receta_planificada",
                        "limite_intentos",
                        "valoracion_receta",
                        "suscripcion_push",
                        "alimento_bedca",
                        "ingrediente_alimento",
                        "foto_receta",
                        "peso_pieza_ingrediente")
                .allSatisfy((tabla, colacion) -> assertThat(colacion).isEqualTo("utf8mb4_0900_ai_ci"));
    }

    @Test
    void laDificultadSoloAdmiteLosTresValoresExactos() {
        assertThat(insertarReceta("Fácil", 10)).isEqualTo(1);
        assertThat(insertarReceta("Media", 10)).isEqualTo(1);
        assertThat(insertarReceta("Difícil", 10)).isEqualTo(1);

        assertThatThrownBy(() -> insertarReceta("facil", 10))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("CK_RECETAS_DIFICULTAD");
        assertThatThrownBy(() -> insertarReceta("Imposible", 10))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("CK_RECETAS_DIFICULTAD");
    }

    @Test
    void elTiempoDebeSerPositivo() {
        assertThatThrownBy(() -> insertarReceta("Fácil", 0))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("CK_RECETAS_TIEMPO");
    }

    @Test
    void elNombreDeIngredienteEsUnicoSinDistinguirMayusculasNiAcentos() {
        int grupo = jdbc.queryForObject("SELECT ID FROM grupo_ingrediente WHERE NOMBRE = 'Esenciales'", Integer.class);

        assertThatThrownBy(() ->
                        jdbc.update("INSERT INTO ingredientes (NOMBRE, GRUPO) VALUES ('aceite de oliva', ?)", grupo))
                .isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO ingredientes (NOMBRE, GRUPO) VALUES ('Limon', ?)", grupo))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void alBorrarUnaRecetaSeBorranSusIngredientesYSusLikes() {
        insertarReceta("Fácil", 10);
        int receta = jdbc.queryForObject("SELECT MAX(ID) FROM recetas", Integer.class);
        jdbc.update(
                "INSERT INTO ingrediente_receta (INGREDIENTE, RECETA, CANTIDAD)"
                        + " SELECT ID, ?, '1 unidad' FROM ingredientes WHERE NOMBRE IN ('Patata', 'Huevos')",
                receta);
        jdbc.update("INSERT INTO like_receta (USUARIO, RECETA) VALUES ('autor', ?)", receta);

        jdbc.update("DELETE FROM recetas WHERE ID = ?", receta);

        assertThat(contar("SELECT COUNT(*) FROM ingrediente_receta WHERE RECETA = ?", receta))
                .isZero();
        assertThat(contar("SELECT COUNT(*) FROM like_receta WHERE RECETA = ?", receta))
                .isZero();
    }

    private int insertarReceta(String dificultad, int tiempo) {
        return jdbc.update(
                "INSERT INTO recetas (TITULO, INSTRUCCIONES, USUARIO, TIPO, TIEMPO, COMENSALES, DIFICULTAD)"
                        + " SELECT 'Receta de prueba', '<p>Pasos</p>', 'autor', ID, ?, 2, ? FROM tipo_receta"
                        + " WHERE NOMBRE = 'Plato'",
                tiempo,
                dificultad);
    }

    private int contar(String sql, Object... parametros) {
        return jdbc.queryForObject(sql, Integer.class, parametros);
    }
}
