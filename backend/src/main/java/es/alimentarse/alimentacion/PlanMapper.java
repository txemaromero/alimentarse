package es.alimentarse.alimentacion;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Acceso a {@code usuario_receta_planificada}. SQL en {@code mybatis/PlanMapper.xml}. */
@Mapper
public interface PlanMapper {

    /** Comidas del usuario entre las dos fechas (incluidas), de la más próxima a la más lejana. */
    List<ComidaPlanificada> comidas(
            @Param("usuario") String usuario, @Param("desde") Instant desde, @Param("hasta") Instant hasta);

    /** {@code SELECT … FOR UPDATE}: dos peticiones sobre la misma comida se ejecutan una detrás de otra. */
    Optional<ComidaPlanificada> bloquear(long id);

    boolean existeHueco(@Param("usuario") String usuario, @Param("fecha") Instant fecha);

    /** Comidas planificadas del usuario desde {@code desde}: las que aún no han llegado (D-089). */
    int contarPendientes(@Param("usuario") String usuario, @Param("desde") Instant desde);

    void insertar(@Param("usuario") String usuario, @Param("receta") int receta, @Param("fecha") Instant fecha);

    int borrar(long id);

    /** Ingredientes de las recetas, con su cantidad y su grupo de alimentos (lista de la compra, D-076). */
    List<IngredienteDeReceta> ingredientesDeRecetas(@Param("recetas") Collection<Integer> recetas);
}
