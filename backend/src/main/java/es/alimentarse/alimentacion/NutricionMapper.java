package es.alimentarse.alimentacion;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Datos del estudio nutricional de las recetas (D-079) y de lo comido en la semana (D-082). */
@Mapper
public interface NutricionMapper {

    List<RecetaComensales> comensales(@Param("recetas") Collection<Integer> recetas);

    /** Ingredientes de las recetas con su composición, si la tienen; los de cada receta, como en el detalle. */
    List<IngredienteNutricion> ingredientes(@Param("recetas") Collection<Integer> recetas);

    /** Pesos de las piezas de los ingredientes de las recetas. */
    List<PesoPieza> piezas(@Param("recetas") Collection<Integer> recetas);

    /** La receta de cada consumo del usuario entre las dos fechas (incluidas): una por consumo. */
    List<Integer> recetasConsumidas(
            @Param("usuario") String usuario, @Param("desde") Instant desde, @Param("hasta") Instant hasta);
}
