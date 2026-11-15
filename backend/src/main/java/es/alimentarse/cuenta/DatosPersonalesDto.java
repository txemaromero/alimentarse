package es.alimentarse.cuenta;

import es.alimentarse.usuario.PerfilDto;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Todo lo que la aplicación guarda del usuario, para descargarlo (D-064). Sin la contraseña ni los tokens.
 *
 * @param exportado cuándo se generó
 * @param recetas sus recetas, publicadas o no (las eliminadas ya no son suyas)
 * @param consumos lo que ha comido, del más antiguo al más reciente
 * @param meGusta las recetas a las que ha dado «me gusta»
 * @param comidasPlanificadas su planificador (D-068)
 * @param valoraciones las notas y comentarios que ha puesto a recetas (D-077)
 * @param avisos los navegadores en los que tiene activados los avisos push (D-078)
 */
public record DatosPersonalesDto(
        Instant exportado,
        PerfilDto perfil,
        List<Receta> recetas,
        List<Actividad> consumos,
        List<Actividad> meGusta,
        List<Actividad> comidasPlanificadas,
        List<Valoracion> valoraciones,
        List<Aviso> avisos) {

    /**
     * @param imagen URL de la foto principal o {@code null}
     * @param fotos las URL de toda la galería, en orden (D-083)
     * @param video el enlace a su vídeo, o {@code null} (D-083)
     */
    public record Receta(
            int id,
            String titulo,
            String tipo,
            String instrucciones,
            int tiempo,
            int comensales,
            String dificultad,
            boolean publicada,
            boolean oculta,
            @Nullable String motivoOculta,
            Instant fechaCreacion,
            @Nullable String imagen,
            List<String> fotos,
            @Nullable String video,
            List<Ingrediente> ingredientes) {}

    public record Ingrediente(String nombre, String cantidad, String grupo) {}

    /** Un consumo, un «me gusta» o una comida planificada. */
    public record Actividad(Instant fecha, int receta, String titulo) {}

    /**
     * Un navegador con los avisos push activados. Solo el dominio del servicio: la dirección completa permite enviarle
     * avisos.
     */
    public record Aviso(Instant fecha, String servicio) {}

    /** @param oculta la ocultó la administración: no la ven los demás ni cuenta en la media */
    public record Valoracion(
            Instant fecha,
            int receta,
            String titulo,
            int nota,
            @Nullable String comentario,
            boolean oculta) {}
}
