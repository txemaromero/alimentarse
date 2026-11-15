package es.alimentarse.receta;

import es.alimentarse.comun.Texto;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Filtro normalizado que usa el XML del mapper ({@code <sql id="filtro">}): los textos ya son patrones {@code LIKE}
 * escapados y {@code null} significa «sin filtro».
 *
 * @param ingredientes ids sin repetir; la receta debe contenerlos todos
 * @param soloPublicadas limitar a recetas publicadas
 * @param autor limitar a las recetas de este usuario (exacto)
 * @param gruposExcluidos ids de grupos: la receta no puede tener ningún ingrediente de ellos (recomendación, 6.3)
 * @param meGustaDe limitar a las recetas a las que este usuario ha dado «me gusta» (D-062)
 * @param soloOcultas limitar a las ocultas por la administración (D-063)
 */
public record FiltroRecetas(
        String titulo,
        Integer tipo,
        String usuario,
        String dificultad,
        Integer comensales,
        Integer tiempoMax,
        List<Integer> ingredientes,
        boolean soloPublicadas,
        String autor,
        List<Integer> gruposExcluidos,
        String meGustaDe,
        boolean soloOcultas) {

    public FiltroRecetas {
        ingredientes = sinNulosNiRepetidos(ingredientes);
        gruposExcluidos = sinNulosNiRepetidos(gruposExcluidos);
    }

    public static FiltroRecetas de(ParametrosFiltroRecetas p) {
        return new FiltroRecetas(
                Texto.patronContiene(p.titulo()),
                p.tipo(),
                Texto.patronContiene(p.usuario()),
                Texto.opcional(p.dificultad()),
                p.comensales(),
                p.tiempoMax(),
                p.ingredientes(),
                false,
                null,
                null,
                null,
                false);
    }

    /** Las ocultas por la administración, publicadas o no (moderación, D-063). */
    public static FiltroRecetas ocultas() {
        return new FiltroRecetas(null, null, null, null, null, null, null, false, null, null, null, true);
    }

    /** Sin más condición que estar publicadas (portada: mejor valoradas y últimas). */
    public static FiltroRecetas todasPublicadas() {
        return new FiltroRecetas(null, null, null, null, null, null, null, true, null, null, null, false);
    }

    public FiltroRecetas soloLasPublicadas() {
        return new FiltroRecetas(
                titulo,
                tipo,
                usuario,
                dificultad,
                comensales,
                tiempoMax,
                ingredientes,
                true,
                autor,
                gruposExcluidos,
                meGustaDe,
                soloOcultas);
    }

    public FiltroRecetas deAutor(String idAutor) {
        return new FiltroRecetas(
                titulo,
                tipo,
                usuario,
                dificultad,
                comensales,
                tiempoMax,
                ingredientes,
                soloPublicadas,
                idAutor,
                gruposExcluidos,
                meGustaDe,
                soloOcultas);
    }

    /** Excluye las recetas que tengan algún ingrediente de estos grupos (los agotados, en la recomendación). */
    public FiltroRecetas sinGrupos(Collection<Integer> grupos) {
        return new FiltroRecetas(
                titulo,
                tipo,
                usuario,
                dificultad,
                comensales,
                tiempoMax,
                ingredientes,
                soloPublicadas,
                autor,
                List.copyOf(grupos),
                meGustaDe,
                soloOcultas);
    }

    /** Solo las recetas a las que este usuario ha dado «me gusta». */
    public FiltroRecetas queLeGustanA(String idUsuario) {
        return new FiltroRecetas(
                titulo,
                tipo,
                usuario,
                dificultad,
                comensales,
                tiempoMax,
                ingredientes,
                soloPublicadas,
                autor,
                gruposExcluidos,
                idUsuario,
                soloOcultas);
    }

    private static List<Integer> sinNulosNiRepetidos(List<Integer> ids) {
        return ids == null
                ? List.of()
                : ids.stream().filter(Objects::nonNull).distinct().toList();
    }
}
