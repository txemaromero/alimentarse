package es.alimentarse.consumo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Acceso a {@code usuario_receta_consumida}. SQL en {@code mybatis/ConsumoMapper.xml}. */
@Mapper
public interface ConsumoMapper {

    void insertar(@Param("usuario") String usuario, @Param("receta") int receta, @Param("fecha") Instant fecha);

    /** Id generado por el último INSERT de esta conexión: solo es fiable dentro de la misma transacción (D-030). */
    long ultimoIdInsertado();

    Optional<Consumo> buscarPorId(long id);

    /** El consumo con la tarjeta de su receta; {@code yo} es el usuario actual (para {@code meGusta}). */
    Optional<ConsumoDto> buscarDto(@Param("id") long id, @Param("yo") String yo);

    /** Consumos del usuario {@code yo}, del más reciente al más antiguo. */
    List<ConsumoDto> historial(
            @Param("yo") String yo, @Param("desplazamiento") long desplazamiento, @Param("limite") int limite);

    long contar(String usuario);

    int actualizarFecha(@Param("id") long id, @Param("fecha") Instant fecha);

    int borrar(long id);
}
