package es.alimentarse.receta;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Acceso a {@code valoracion_receta} (D-077). SQL en {@code mybatis/ValoracionMapper.xml}. */
@Mapper
public interface ValoracionMapper {

    /** La del usuario en esa receta, oculta o no. */
    Optional<ValoracionDto> deUsuario(@Param("receta") int receta, @Param("usuario") String usuario);

    /** De la más reciente a la más antigua; con {@code conOcultas}, también las ocultas (administradores). */
    List<ValoracionDto> deReceta(
            @Param("receta") int receta,
            @Param("conOcultas") boolean conOcultas,
            @Param("desplazamiento") long desplazamiento,
            @Param("limite") int limite);

    long contar(@Param("receta") int receta, @Param("conOcultas") boolean conOcultas);

    /** Media y número de las visibles. */
    NotaMedia notaMedia(int receta);

    /** Crea la del usuario o la sustituye. Si la administración la había ocultado, sigue oculta. */
    void guardar(
            @Param("usuario") String usuario,
            @Param("receta") int receta,
            @Param("nota") int nota,
            @Param("comentario") String comentario,
            @Param("fecha") Instant fecha);

    int borrar(@Param("receta") int receta, @Param("usuario") String usuario);

    Optional<ValoracionDto> buscar(long id);

    int actualizarOculta(@Param("id") long id, @Param("oculta") boolean oculta);
}
