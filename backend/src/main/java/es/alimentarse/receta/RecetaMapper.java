package es.alimentarse.receta;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Acceso a {@code recetas}, {@code ingrediente_receta} y {@code like_receta}. SQL en {@code mybatis/RecetaMapper.xml}. */
@Mapper
public interface RecetaMapper {

    /**
     * @param yo usuario actual, para calcular {@code meGusta}
     */
    List<RecetaResumenDto> buscar(
            @Param("filtro") FiltroRecetas filtro,
            @Param("yo") String yo,
            @Param("orden") OrdenRecetas orden,
            @Param("desplazamiento") long desplazamiento,
            @Param("limite") int limite);

    /**
     * Como {@link #buscar}, pero primero las recetas sin ningún grupo de {@code consumidos}, después las que no tienen
     * ninguno de {@code cercaDelLimite} y, dentro de cada tramo, por «me gusta» y fecha (D-067).
     */
    List<RecetaResumenDto> buscarPorVariedad(
            @Param("filtro") FiltroRecetas filtro,
            @Param("yo") String yo,
            @Param("consumidos") Collection<Integer> consumidos,
            @Param("cercaDelLimite") Collection<Integer> cercaDelLimite,
            @Param("desplazamiento") long desplazamiento,
            @Param("limite") int limite);

    long contar(@Param("filtro") FiltroRecetas filtro);

    /** Tarjetas de las recetas indicadas, sin orden ni filtro de visibilidad (las elige quien llama). */
    List<RecetaResumenDto> buscarPorIds(@Param("ids") Collection<Integer> ids, @Param("yo") String yo);

    /** Recetas publicadas cuyo título contiene el texto; primero las que empiezan por él. */
    List<SugerenciaRecetaDto> sugerencias(
            @Param("prefijo") String patronPrefijo,
            @Param("contiene") String patronContiene,
            @Param("limite") int limite);

    Optional<RecetaBase> buscarBase(@Param("id") int id, @Param("yo") String yo);

    List<IngredienteRecetaDto> ingredientesDe(int receta);

    int contarIngredientesExistentes(@Param("ids") Collection<Integer> ids);

    void insertar(
            @Param("titulo") String titulo,
            @Param("instrucciones") String instrucciones,
            @Param("usuario") String usuario,
            @Param("tipo") int tipo,
            @Param("imagen") String imagen,
            @Param("tiempo") int tiempo,
            @Param("comensales") int comensales,
            @Param("dificultad") String dificultad);

    /** Id generado por el último INSERT de esta conexión: solo es fiable dentro de la misma transacción (D-030). */
    int ultimoIdInsertado();

    /** Recetas del autor que no ha eliminado (D-089). */
    int contarDelAutor(String usuario);

    int actualizar(
            @Param("id") int id,
            @Param("titulo") String titulo,
            @Param("instrucciones") String instrucciones,
            @Param("tipo") int tipo,
            @Param("imagen") String imagen,
            @Param("tiempo") int tiempo,
            @Param("comensales") int comensales,
            @Param("dificultad") String dificultad);

    int actualizarPublicada(@Param("id") int id, @Param("publicada") boolean publicada);

    /** Moderación (D-063); {@code motivo} es {@code null} al volver a mostrarla. */
    int actualizarOculta(@Param("id") int id, @Param("oculta") boolean oculta, @Param("motivo") String motivo);

    /** {@code SELECT … FOR UPDATE} de la receta; {@code null} si no existe. */
    Integer bloquear(int id);

    /** ¿La ha consumido alguien, sea quien sea? */
    boolean tieneConsumos(int receta);

    /** Borrado lógico (D-061). */
    int marcarEliminada(int id);

    void borrarLikes(int receta);

    /** Borrado definitivo, con sus ingredientes y sus «me gusta». */
    int borrar(int id);

    void borrarIngredientes(int receta);

    /** El enlace al vídeo de la receta, o {@code null} (D-083). */
    String videoDe(int receta);

    void guardarVideo(@Param("receta") int receta, @Param("video") String video);

    /** La galería de la receta, en orden (D-083). */
    List<FotoReceta> fotosDe(int receta);

    int borrarFotos(int receta);

    void insertarFotos(@Param("receta") int receta, @Param("fotos") List<FotoReceta> fotos);

    void insertarIngredientes(
            @Param("receta") int receta, @Param("ingredientes") List<IngredienteCantidad> ingredientes);

    /** Idempotente: si ya le había dado «me gusta», no cambia nada (tampoco la fecha). */
    void darLike(@Param("usuario") String usuario, @Param("receta") int receta, @Param("fecha") Instant fecha);

    void quitarLike(@Param("usuario") String usuario, @Param("receta") int receta);

    long contarLikes(int receta);
}
