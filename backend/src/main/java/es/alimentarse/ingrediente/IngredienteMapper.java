package es.alimentarse.ingrediente;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Acceso a {@code ingredientes}. SQL en {@code mybatis/IngredienteMapper.xml}. Los parámetros de texto de búsqueda son
 * patrones {@code LIKE} ya escapados ({@link es.alimentarse.comun.Texto}); {@code null} = sin filtro.
 */
@Mapper
public interface IngredienteMapper {

    List<IngredienteDto> buscar(
            @Param("nombre") String patronNombre,
            @Param("grupo") Integer grupo,
            @Param("sinAlimento") boolean sinAlimento,
            @Param("desplazamiento") long desplazamiento,
            @Param("limite") int limite);

    long contar(
            @Param("nombre") String patronNombre,
            @Param("grupo") Integer grupo,
            @Param("sinAlimento") boolean sinAlimento);

    /** Los que empiezan por {@code prefijo} primero, después los que contienen el texto; por nombre. */
    List<IngredienteDto> sugerencias(
            @Param("prefijo") String patronPrefijo,
            @Param("contiene") String patronContiene,
            @Param("limite") int limite);

    Optional<IngredienteDto> buscarPorId(int id);

    /** ¿Hay otro ingrediente con ese nombre (sin distinguir mayúsculas ni acentos)? */
    boolean existeNombre(@Param("nombre") String nombre, @Param("exceptoId") Integer exceptoId);

    void insertar(@Param("nombre") String nombre, @Param("grupo") int grupo);

    /** Id generado por el último INSERT de esta conexión: solo es fiable dentro de la misma transacción (D-030). */
    int ultimoIdInsertado();

    int actualizar(@Param("id") int id, @Param("nombre") String nombre, @Param("grupo") int grupo);
}
