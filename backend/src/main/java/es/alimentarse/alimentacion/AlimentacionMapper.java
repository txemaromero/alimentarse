package es.alimentarse.alimentacion;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Consultas de estadísticas sobre los consumos. SQL en {@code mybatis/AlimentacionMapper.xml}. */
@Mapper
public interface AlimentacionMapper {

    /** Una fila por cada consumo del usuario entre las dos fechas (incluidas) y cada grupo de su receta. */
    List<ConsumoGrupo> consumosConGrupos(
            @Param("usuario") String usuario, @Param("desde") Instant desde, @Param("hasta") Instant hasta);

    /** Una fila por cada receta indicada y cada grupo de sus ingredientes. {@code recetas} no puede estar vacío. */
    List<GrupoDeReceta> gruposDeRecetas(@Param("recetas") Collection<Integer> recetas);

    /** Recetas que más veces ha consumido el usuario; a igualdad, la consumida más recientemente. */
    List<RecetaVeces> masConsumidas(@Param("usuario") String usuario, @Param("limite") int limite);

    /** Ingredientes presentes en más consumos del usuario; a igualdad, por nombre. */
    List<IngredienteVeces> ingredientesMasUsados(@Param("usuario") String usuario, @Param("limite") int limite);
}
