package es.alimentarse.seguridad;

import java.time.Instant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Ventanas de {@code limite_intentos}. SQL en {@code mybatis/LimiteIntentosMapper.xml}. */
@Mapper
public interface LimiteIntentosMapper {

    /** Ventana actual de una clave. */
    record Ventana(Instant fin, int intentos) {}

    /**
     * Suma un intento a la clave, de forma atómica aunque haya varios servidores. Si su ventana ha terminado, o no
     * tenía, abre una nueva que acaba en {@code fin}.
     */
    void registrar(@Param("clave") String clave, @Param("ahora") Instant ahora, @Param("fin") Instant fin);

    Ventana ventana(String clave);

    void borrar(String clave);

    /** Borra las ventanas terminadas. */
    int limpiar(@Param("ahora") Instant ahora);
}
