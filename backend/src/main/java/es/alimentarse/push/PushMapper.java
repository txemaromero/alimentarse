package es.alimentarse.push;

import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Suscripciones push y recordatorios de las comidas planificadas (D-078). SQL en {@code mybatis/PushMapper.xml}. */
@Mapper
public interface PushMapper {

    /**
     * Crea la suscripción o, si ese navegador ya estaba suscrito (mismo endpoint), la actualiza y la pasa al usuario
     * actual: en un equipo compartido, los avisos son de quien se suscribió el último.
     */
    void guardar(
            @Param("usuario") String usuario,
            @Param("endpoint") String endpoint,
            @Param("hash") String hash,
            @Param("p256dh") String p256dh,
            @Param("auth") String auth,
            @Param("zona") String zona,
            @Param("fecha") Instant fecha);

    int borrar(@Param("usuario") String usuario, @Param("hash") String hash);

    void borrarPorId(long id);

    List<SuscripcionPush> deUsuario(String usuario);

    /** Navegadores suscritos del usuario, sin contar el de {@code hash} (D-089). */
    int contarOtros(@Param("usuario") String usuario, @Param("hash") String hash);

    /**
     * Comidas entre las dos fechas sin recordatorio, de usuarios no bloqueados con alguna suscripción y con la receta
     * aún disponible para ellos (las mismas reglas que el planificador, D-068).
     */
    List<ComidaPorAvisar> comidasPorAvisar(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    /** Marca el recordatorio como enviado si nadie lo ha hecho antes: 1 si esta llamada se queda con él. */
    int reclamar(@Param("id") long id, @Param("ahora") Instant ahora);
}
