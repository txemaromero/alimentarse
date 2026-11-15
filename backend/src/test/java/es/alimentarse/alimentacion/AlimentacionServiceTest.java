package es.alimentarse.alimentacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import es.alimentarse.catalogo.GrupoDto;
import es.alimentarse.catalogo.TipoRecetaDto;
import es.alimentarse.consumo.ConsumoDto;
import es.alimentarse.consumo.ConsumoService;
import es.alimentarse.ingrediente.IngredienteDto;
import es.alimentarse.receta.RecetaResumenDto;
import es.alimentarse.receta.RecetaService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlimentacionServiceTest {

    private static final Instant FECHA = Instant.parse("2026-11-05T10:00:00Z");

    @Mock
    ConsumoService consumos;

    @Mock
    AlimentacionMapper alimentacion;

    @Mock
    RecetaService recetas;

    @Mock
    RecomendacionService recomendacion;

    @Test
    void elResumenReuneUltimasMasConsumidasIngredientesEIndicadores() {
        RecetaResumenDto pollo = tarjeta(6, "Pollo con tallarines");
        RecetaResumenDto batido = tarjeta(10, "Batido");
        ConsumoDto ultimo = new ConsumoDto(1, FECHA, batido);
        IndicadorGrupoDto indicador =
                new IndicadorGrupoDto(new GrupoDto(11, "Carne", 3), 3, 3, 100, ColorIndicador.ROJO);
        when(consumos.ultimos("lucia", 3)).thenReturn(List.of(ultimo));
        when(alimentacion.masConsumidas("lucia", 3)).thenReturn(List.of(new RecetaVeces(6, 3), new RecetaVeces(10, 2)));
        when(recetas.resumenes(List.of(6, 10), "lucia")).thenReturn(Map.of(10, batido, 6, pollo));
        when(alimentacion.ingredientesMasUsados("lucia", 4))
                .thenReturn(List.of(new IngredienteVeces(30, "Cebolla", 4, "Hortalizas", null, "Cebolla", 3)));
        when(recomendacion.indicadores("lucia")).thenReturn(List.of(indicador));

        ResumenAlimentacionDto resumen =
                new AlimentacionService(consumos, alimentacion, recetas, recomendacion).resumen("lucia");

        assertThat(resumen.ultimas()).containsExactly(ultimo);
        assertThat(resumen.masConsumidas())
                .containsExactly(new RecetaConsumidaDto(pollo, 3), new RecetaConsumidaDto(batido, 2));
        assertThat(resumen.ingredientesMasUsados())
                .containsExactly(new IngredienteUsadoDto(
                        new IngredienteDto(30, "Cebolla", new GrupoDto(4, "Hortalizas", null), "Cebolla"), 3));
        assertThat(resumen.grupos7Dias()).containsExactly(indicador);
    }

    private static RecetaResumenDto tarjeta(int id, String titulo) {
        return new RecetaResumenDto(
                id,
                titulo,
                "lucia",
                new TipoRecetaDto(1, "Plato"),
                null,
                10,
                2,
                "Fácil",
                0,
                false,
                null,
                0,
                true,
                FECHA,
                false,
                false);
    }
}
