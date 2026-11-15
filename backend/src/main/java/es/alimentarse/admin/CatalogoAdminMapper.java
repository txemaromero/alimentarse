package es.alimentarse.admin;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Grupos de alimentos e ingredientes en la administración (D-065). SQL en {@code mybatis/CatalogoAdminMapper.xml}. */
@Mapper
public interface CatalogoAdminMapper {

    /** Por nombre, con cuántos ingredientes usa cada uno. */
    List<GrupoAdminDto> grupos();

    Optional<GrupoAdminDto> grupoPorId(int id);

    /** ¿Usa otro grupo este nombre? {@code exceptoId} excluye al propio grupo (puede ser {@code null}). */
    boolean existeNombreGrupo(@Param("nombre") String nombre, @Param("exceptoId") Integer exceptoId);

    void insertarGrupo(
            @Param("nombre") String nombre,
            @Param("racionesSemana") Integer racionesSemana,
            @Param("basico") boolean basico);

    /** Id generado por el último INSERT de esta conexión (D-030). */
    int ultimoIdInsertado();

    int actualizarGrupo(
            @Param("id") int id,
            @Param("nombre") String nombre,
            @Param("racionesSemana") Integer racionesSemana,
            @Param("basico") boolean basico);

    /** {@code SELECT … FOR UPDATE}: un ingrediente nuevo en este grupo espera a que se decida si se borra. */
    List<Integer> bloquearGrupo(int id);

    long contarIngredientesDeGrupo(int id);

    int borrarGrupo(int id);

    /** {@code SELECT … FOR UPDATE} de los dos ingredientes, siempre en el mismo orden para no causar interbloqueos. */
    List<Integer> bloquearIngredientes(@Param("a") int a, @Param("b") int b);

    /** Recetas (también las eliminadas) que llevan el ingrediente. */
    int contarRecetasConIngrediente(int ingrediente);

    /** En las recetas que llevan los dos, la cantidad del origen se suma a la del destino («2 + 1 unidad»). */
    int sumarCantidades(@Param("origen") int origen, @Param("destino") int destino);

    /** Después de {@link #sumarCantidades}: quita el origen de las recetas que ya llevan el destino. */
    int borrarLineasRepetidas(@Param("origen") int origen, @Param("destino") int destino);

    /** El resto de recetas pasan del origen al destino. */
    int moverLineas(@Param("origen") int origen, @Param("destino") int destino);

    int borrarIngrediente(int id);

    /**
     * El destino se queda con el alimento de BEDCA del origen si no tenía uno (D-080). Se llama antes de borrar el
     * origen.
     */
    int heredarAlimento(@Param("origen") int origen, @Param("destino") int destino);

    /** Y con los pesos de las piezas del origen que no tenga (D-079). */
    int heredarPiezas(@Param("origen") int origen, @Param("destino") int destino);

    /**
     * Alimentos de BEDCA cuyo nombre contiene todas las palabras (patrones {@code LIKE} ya escapados): primero los que
     * empiezan por el texto, después por nombre.
     */
    List<AlimentoBedca> buscarAlimentos(
            @Param("palabras") List<String> patronesPalabras,
            @Param("prefijo") String patronPrefijo,
            @Param("limite") int limite);

    boolean existeAlimento(int id);

    /** Asigna el alimento al ingrediente, o lo cambia si ya tenía uno. */
    void asignarAlimento(@Param("ingrediente") int ingrediente, @Param("alimento") int alimento);

    int quitarAlimento(int ingrediente);

    /** El alimento de BEDCA del ingrediente, si tiene. */
    Optional<AlimentoBedca> alimentoDe(int ingrediente);

    /** Los pesos de las piezas del ingrediente, por orden alfabético (D-081). */
    List<PiezaDto> piezasDe(int ingrediente);

    int borrarPiezas(int ingrediente);

    void insertarPiezas(@Param("ingrediente") int ingrediente, @Param("piezas") List<PiezaDto> piezas);
}
