package es.alimentarse.basedatos;

import static org.assertj.core.api.Assertions.assertThat;

import es.alimentarse.support.TestIntegracion;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Datos iniciales de V2__seed.sql y la composición nutricional de V13 y V14 (D-079, D-080). */
@TestIntegracion
class DatosSemillaTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void existenLosRolesUserYAdmin() {
        assertThat(jdbc.queryForList("SELECT NOMBRE FROM roles", String.class))
                .containsExactlyInAnyOrder("USER", "ADMIN");
    }

    @Test
    void existenLosSeisTiposDeReceta() {
        assertThat(jdbc.queryForList("SELECT NOMBRE FROM tipo_receta", String.class))
                .containsExactlyInAnyOrder("Plato", "Entrante", "Postre", "Bebida", "Desayuno", "Salsa");
    }

    @Test
    void losGruposTienenLasRacionesSemanalesDeLaEspecificacion() {
        Map<String, Integer> esperadas = new LinkedHashMap<>();
        esperadas.put("Esenciales", null);
        esperadas.put("Especias", null);
        esperadas.put("Verduras", null);
        esperadas.put("Hortalizas", null);
        esperadas.put("Fruta", null);
        esperadas.put("Legumbres", 4);
        esperadas.put("Pasta", 4);
        esperadas.put("Arroz", 4);
        esperadas.put("Pescado", 4);
        esperadas.put("Huevos", 4);
        esperadas.put("Carne", 3);
        esperadas.put("Carne grasa", 2);
        esperadas.put("Leches y derivados", 14);
        esperadas.put("Frutos secos", 7);
        esperadas.put("Dulces", 1);

        Map<String, Integer> racionesPorGrupo = new LinkedHashMap<>();
        jdbc.query("SELECT NOMBRE, RACIONES_SEMANA FROM grupo_ingrediente", fila -> {
            racionesPorGrupo.put(fila.getString("NOMBRE"), fila.getObject("RACIONES_SEMANA", Integer.class));
        });

        assertThat(racionesPorGrupo).isEqualTo(esperadas);
    }

    @Test
    void soloEsencialesYEspeciasSonGruposBasicos() {
        assertThat(jdbc.queryForList("SELECT NOMBRE FROM grupo_ingrediente WHERE BASICO", String.class))
                .containsExactlyInAnyOrder("Esenciales", "Especias");
    }

    @Test
    void hayAlMenosSesentaIngredientesYTodosLosGruposTienenAlguno() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ingredientes", Integer.class))
                .isGreaterThanOrEqualTo(60);
        assertThat(jdbc.queryForList(
                        "SELECT g.NOMBRE FROM grupo_ingrediente g"
                                + " WHERE NOT EXISTS (SELECT 1 FROM ingredientes i WHERE i.GRUPO = g.ID)",
                        String.class))
                .isEmpty();
    }

    @Test
    void estanTodosLosIngredientesQueNombraLaEspecificacion() {
        List<String> nombrados = List.of(
                "Aceite de oliva",
                "Aceite de girasol",
                "Sal",
                "Agua",
                "Azúcar",
                "Azúcar moreno",
                "Canela",
                "Pimienta",
                "Patata",
                "Cebolla",
                "Ajo",
                "Calabaza",
                "Puerro",
                "Berenjena",
                "Tomate",
                "Espárragos",
                "Atún",
                "Boquerón",
                "Sardina",
                "Merluza",
                "Pollo",
                "Carrillada",
                "Ternera",
                "Bacon",
                "Chorizo",
                "Huevos",
                "Leche semidesnatada",
                "Queso",
                "Nata",
                "Mantequilla",
                "Harina",
                "Tallarines",
                "Macarrones",
                "Arroz",
                "Lentejas",
                "Garbanzos",
                "Cacahuete",
                "Nueces",
                "Chocolate blanco",
                "Limón",
                "Manzana",
                "Plátano");

        assertThat(jdbc.queryForList("SELECT NOMBRE FROM ingredientes", String.class))
                .containsAll(nombrados);
    }

    @ParameterizedTest
    @CsvSource({
        "Azúcar, Esenciales",
        "Harina, Esenciales",
        "Patata, Hortalizas",
        "Espárragos, Verduras",
        "Limón, Fruta",
        "Chocolate blanco, Dulces",
        "Bacon, Carne grasa",
        "Pollo, Carne",
        "Queso, Leches y derivados"
    })
    void cadaIngredienteEstaEnSuGrupo(String ingrediente, String grupo) {
        assertThat(jdbc.queryForObject(
                        "SELECT g.NOMBRE FROM ingredientes i JOIN grupo_ingrediente g ON g.ID = i.GRUPO"
                                + " WHERE i.NOMBRE = ?",
                        String.class,
                        ingrediente))
                .isEqualTo(grupo);
    }

    @Test
    void todosLosIngredientesDelCatalogoTienenComposicionMenosLaLevaduraQueNoEstaEnBedca() {
        assertThat(jdbc.queryForList(
                        "SELECT i.NOMBRE FROM ingredientes i"
                                + " WHERE NOT EXISTS (SELECT 1 FROM ingrediente_alimento a WHERE a.INGREDIENTE = i.ID)",
                        String.class))
                .containsExactly("Levadura");
    }

    @Test
    void laCarrilladaUsaLaCabezadaDeCerdoYPesaUnosCientoVeinteGramos() {
        // BEDCA no publica la carrillera de vacuno: se usa el corte de cerdo más parecido (D-084)
        assertThat(jdbc.queryForObject(
                        "SELECT a.NOMBRE FROM ingrediente_alimento ia"
                                + " JOIN ingredientes i ON i.ID = ia.INGREDIENTE JOIN alimento_bedca a ON a.ID = ia.ALIMENTO"
                                + " WHERE i.NOMBRE = 'Carrillada'",
                        String.class))
                .isEqualTo("Carne de cerdo de capa blanca, cabezada, partes grasa y magra, crudo");
        assertThat(jdbc.queryForObject(
                        "SELECT p.GRAMOS FROM peso_pieza_ingrediente p JOIN ingredientes i ON i.ID = p.INGREDIENTE"
                                + " WHERE i.NOMBRE = 'Carrillada' AND p.PIEZA = 'unidad'",
                        BigDecimal.class))
                .isEqualByComparingTo("120");
    }

    @Test
    void laComposicionEsLaDeBedcaSinCambios() {
        Map<String, Object> huevo =
                jdbc.queryForMap("SELECT a.ID AS BEDCA_ID, a.NOMBRE AS BEDCA_NOMBRE, a.* FROM ingrediente_alimento ia"
                        + " JOIN ingredientes i ON i.ID = ia.INGREDIENTE JOIN alimento_bedca a ON a.ID = ia.ALIMENTO"
                        + " WHERE i.NOMBRE = 'Huevos'");

        assertThat(huevo.get("BEDCA_ID")).isEqualTo(2127);
        assertThat(huevo.get("BEDCA_NOMBRE")).isEqualTo("Huevo de gallina fresco");
        assertThat((BigDecimal) huevo.get("ENERGIA_KJ")).isEqualByComparingTo("623.2");
        assertThat((BigDecimal) huevo.get("PROTEINAS")).isEqualByComparingTo("12.5");
        assertThat((BigDecimal) huevo.get("SODIO_MG")).isEqualByComparingTo("140");
        // BEDCA no da los hidratos del huevo: no se inventa un cero
        assertThat(huevo.get("HIDRATOS")).isNull();
    }

    @Test
    void lasPiezasTienenUnPesoAproximado() {
        assertThat(jdbc.queryForObject(
                        "SELECT p.GRAMOS FROM peso_pieza_ingrediente p JOIN ingredientes i ON i.ID = p.INGREDIENTE"
                                + " WHERE i.NOMBRE = 'Ajo' AND p.PIEZA = 'diente'",
                        BigDecimal.class))
                .isEqualByComparingTo("5");
    }

    @Test
    void elCatalogoDeBedcaEstaCompletoYLaPimientaEnKj() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM alimento_bedca", Integer.class))
                .isGreaterThan(900);
        // BEDCA da la energía de la pimienta en kcal (230): se guarda su equivalente en kJ
        assertThat(jdbc.queryForObject("SELECT ENERGIA_KJ FROM alimento_bedca WHERE ID = 2590", BigDecimal.class))
                .isEqualByComparingTo("962.32");
    }
}
