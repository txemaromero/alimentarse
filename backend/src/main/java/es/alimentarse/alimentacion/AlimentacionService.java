package es.alimentarse.alimentacion;

import es.alimentarse.consumo.ConsumoService;
import es.alimentarse.receta.RecetaResumenDto;
import es.alimentarse.receta.RecetaService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resumen de la pantalla «Mi alimentación». */
@Service
public class AlimentacionService {

    static final int ULTIMAS = 3;
    static final int MAS_CONSUMIDAS = 3;
    static final int INGREDIENTES_MAS_USADOS = 4;

    private final ConsumoService consumos;
    private final AlimentacionMapper alimentacion;
    private final RecetaService recetas;
    private final RecomendacionService recomendacion;

    public AlimentacionService(
            ConsumoService consumos,
            AlimentacionMapper alimentacion,
            RecetaService recetas,
            RecomendacionService recomendacion) {
        this.consumos = consumos;
        this.alimentacion = alimentacion;
        this.recetas = recetas;
        this.recomendacion = recomendacion;
    }

    /** Las recetas más consumidas y los ingredientes más usados se calculan sobre todo el historial (D-040). */
    @Transactional(readOnly = true)
    public ResumenAlimentacionDto resumen(String yo) {
        return new ResumenAlimentacionDto(
                consumos.ultimos(yo, ULTIMAS),
                masConsumidas(yo),
                alimentacion.ingredientesMasUsados(yo, INGREDIENTES_MAS_USADOS).stream()
                        .map(IngredienteVeces::aDto)
                        .toList(),
                recomendacion.indicadores(yo));
    }

    private List<RecetaConsumidaDto> masConsumidas(String yo) {
        List<RecetaVeces> veces = alimentacion.masConsumidas(yo, MAS_CONSUMIDAS);
        Map<Integer, RecetaResumenDto> tarjetas =
                recetas.resumenes(veces.stream().map(RecetaVeces::receta).toList(), yo);
        return veces.stream()
                .filter(v -> tarjetas.containsKey(v.receta()))
                .map(v -> new RecetaConsumidaDto(tarjetas.get(v.receta()), v.veces()))
                .toList();
    }
}
