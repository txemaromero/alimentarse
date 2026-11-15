package es.alimentarse.cuenta;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/** Exportación de los datos personales y baja de la cuenta (D-064). SQL en {@code mybatis/CuentaMapper.xml}. */
@Mapper
public interface CuentaMapper {

    /** Recetas propias que no están eliminadas, de la más antigua a la más reciente. */
    List<FilaReceta> recetasDe(String usuario);

    /** Ingredientes de esas recetas, por receta y nombre. */
    List<FilaIngrediente> ingredientesDe(String usuario);

    List<FilaActividad> consumosDe(String usuario);

    List<FilaActividad> meGustaDe(String usuario);

    List<FilaActividad> planDe(String usuario);
    /** Sus valoraciones, de la más antigua a la más reciente (D-077). */
    List<FilaValoracion> valoracionesDe(String usuario);
    /** Sus suscripciones a los avisos push (D-078). */
    List<FilaSuscripcion> suscripcionesDe(String usuario);

    /** {@code SELECT … FOR UPDATE} de sus recetas, antes de cualquier otra lectura (como en D-061). */
    List<Integer> bloquearRecetasDe(String usuario);

    /** Los ficheros de sus recetas: las principales y las de las galerías (D-083). */
    List<String> imagenesDe(String usuario);

    /** Las fotos de sus recetas, por receta y en orden (D-083). */
    List<FilaFoto> fotosDe(String usuario);

    /** Antes de anonimizar sus recetas: las consumidas por otros se quedan sin fotos (D-083). */
    void borrarFotosDe(String usuario);

    void borrarConsumosDe(String usuario);

    void borrarPlanDe(String usuario);

    void borrarLikesDe(String usuario);

    void borrarLikesDeSusRecetas(String usuario);

    void borrarValoracionesDe(String usuario);

    void borrarSuscripcionesDe(String usuario);

    /** Las que alguien ha consumido: eliminadas, sin autor, sin imagen y sin instrucciones (D-064). */
    int anonimizarRecetasConsumidas(String usuario);

    /** Las demás, con sus ingredientes (en cascada). */
    int borrarRecetasDe(String usuario);

    void borrarRolesDe(String usuario);

    int borrarUsuario(String usuario);

    /** Fila de una receta propia para la exportación. */
    record FilaReceta(
            int id,
            String titulo,
            String tipo,
            String instrucciones,
            int tiempo,
            int comensales,
            String dificultad,
            boolean publicada,
            boolean oculta,
            String motivoOculta,
            java.time.Instant fechaCreacion,
            String imagen,
            String video) {}

    record FilaIngrediente(int receta, String nombre, String cantidad, String grupo) {}

    record FilaFoto(int receta, String nombre) {}

    /** Un consumo o un «me gusta»: cuándo y de qué receta. */
    record FilaActividad(java.time.Instant fecha, int receta, String titulo) {}

    /** Una suscripción a los avisos push (D-078): cuándo y a qué servicio. */
    record FilaSuscripcion(java.time.Instant fecha, String endpoint) {}

    /** Una valoración propia (D-077). */
    record FilaValoracion(
            java.time.Instant fecha, int receta, String titulo, int nota, String comentario, boolean oculta) {}
}
