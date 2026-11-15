package es.alimentarse.receta;

import es.alimentarse.catalogo.CatalogoMapper;
import es.alimentarse.catalogo.GrupoDto;
import es.alimentarse.comun.Pagina;
import es.alimentarse.comun.SaneadorHtml;
import es.alimentarse.comun.Texto;
import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.imagen.ImagenService;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Recetas (6.1, 6.5, 6.6): listados, detalle, alta, edición con imagen, publicación, eliminación y «me gusta». Una
 * receta no publicada solo existe para su autor: para los demás es un 404 (D-032). Una eliminada no existe para nadie
 * (D-061). Una oculta por la administración solo existe para su autor y para los administradores (D-063).
 */
@Service
public class RecetaService {

    static final int MAXIMO_SUGERENCIAS = 8;
    static final int LONGITUD_MAXIMA_HTML = 60_000;
    static final String TIPO_INEXISTENTE = "El tipo de receta no existe";
    static final String INGREDIENTES_REPETIDOS = "Hay ingredientes repetidos";
    static final String INGREDIENTE_INEXISTENTE = "Algún ingrediente no existe";
    static final String INSTRUCCIONES_VACIAS = "Las instrucciones no pueden quedar vacías";
    static final String FOTO_SIN_ORIGEN = "Indica una foto que ya tiene la receta o un fichero nuevo, no las dos";
    static final String FOTO_AJENA = "Esa foto no es de esta receta";
    static final String FOTO_REPETIDA = "Esta foto está repetida";
    static final String FICHERO_QUE_FALTA = "Falta el fichero de esta foto";
    static final String FICHEROS_SOBRANTES = "Hay ficheros que no corresponden a ninguna foto";
    static final String VIDEO_NO_VALIDO = "Pega el enlace de un vídeo de YouTube o Vimeo (https://…)";

    /** Dónde puede estar el vídeo de una receta (D-083). */
    static final Set<String> SERVIDORES_VIDEO = Set.of(
            "youtube.com",
            "www.youtube.com",
            "m.youtube.com",
            "youtu.be",
            "vimeo.com",
            "www.vimeo.com",
            "player.vimeo.com");

    static final String MOTIVO_OBLIGATORIO = "Indica el motivo";
    static final String RECETA_OCULTA =
            "La administración ha ocultado esta receta: no se puede publicar mientras siga oculta.";

    /** Recetas por autor, como mucho (D-089); las eliminadas no cuentan. */
    static final int MAXIMO_RECETAS = 500;

    static final String DEMASIADAS_RECETAS = "Has llegado al máximo de 500 recetas: elimina alguna para crear otra.";

    private final RecetaMapper recetas;
    private final ValoracionMapper valoraciones;
    private final CatalogoMapper catalogo;
    private final ImagenService imagenes;
    private final SaneadorHtml saneador;
    private final Clock reloj;

    public RecetaService(
            RecetaMapper recetas,
            ValoracionMapper valoraciones,
            CatalogoMapper catalogo,
            ImagenService imagenes,
            SaneadorHtml saneador,
            Clock reloj) {
        this.recetas = recetas;
        this.valoraciones = valoraciones;
        this.catalogo = catalogo;
        this.imagenes = imagenes;
        this.saneador = saneador;
        this.reloj = reloj;
    }

    /** Recetas publicadas que cumplen los filtros, de la más reciente a la más antigua (D-031). */
    @Transactional(readOnly = true)
    public Pagina<RecetaResumenDto> listar(FiltroRecetas filtro, String yo, int pagina, int tamano) {
        return buscarPagina(filtro.soloLasPublicadas(), yo, OrdenRecetas.RECIENTES, pagina, tamano);
    }

    /** Recetas propias, publicadas o no. */
    @Transactional(readOnly = true)
    public Pagina<RecetaResumenDto> mias(FiltroRecetas filtro, String yo, int pagina, int tamano) {
        return buscarPagina(filtro.deAutor(yo), yo, OrdenRecetas.RECIENTES, pagina, tamano);
    }

    /**
     * «Recetas que me gustan» (D-062): las publicadas a las que el usuario ha dado «me gusta», de la que le gustó más
     * recientemente a la más antigua.
     */
    @Transactional(readOnly = true)
    public Pagina<RecetaResumenDto> meGustan(FiltroRecetas filtro, String yo, int pagina, int tamano) {
        return buscarPagina(
                filtro.soloLasPublicadas().queLeGustanA(yo), yo, OrdenRecetas.ME_GUSTA_RECIENTES, pagina, tamano);
    }

    /**
     * Página de recetas con el filtro y el orden indicados, tal cual: quien llama decide si limita a las publicadas o a
     * un autor (lo usa la recomendación, 6.3).
     */
    @Transactional(readOnly = true)
    public Pagina<RecetaResumenDto> buscarPagina(
            FiltroRecetas filtro, String yo, OrdenRecetas orden, int pagina, int tamano) {
        long total = recetas.contar(filtro);
        long desplazamiento = Pagina.desplazamiento(pagina, tamano);
        List<RecetaResumenDto> contenido =
                desplazamiento >= total ? List.of() : recetas.buscar(filtro, yo, orden, desplazamiento, tamano);
        return Pagina.de(contenido, pagina, tamano, total);
    }

    /**
     * Página de la recomendación: el filtro tal cual, dejando para el final las recetas de los grupos ya consumidos esta
     * semana y, detrás de todas, las de los grupos cerca de su límite (D-067).
     */
    @Transactional(readOnly = true)
    public Pagina<RecetaResumenDto> buscarPaginaPorVariedad(
            FiltroRecetas filtro,
            String yo,
            Collection<Integer> consumidos,
            Collection<Integer> cercaDelLimite,
            int pagina,
            int tamano) {
        long total = recetas.contar(filtro);
        long desplazamiento = Pagina.desplazamiento(pagina, tamano);
        List<RecetaResumenDto> contenido = desplazamiento >= total
                ? List.of()
                : recetas.buscarPorVariedad(filtro, yo, consumidos, cercaDelLimite, desplazamiento, tamano);
        return Pagina.de(contenido, pagina, tamano, total);
    }

    /**
     * Tarjetas de las recetas indicadas, por id. No filtra por visibilidad: las usa el resumen de alimentación para
     * recetas que el propio usuario ya ha consumido.
     */
    @Transactional(readOnly = true)
    public Map<Integer, RecetaResumenDto> resumenes(Collection<Integer> ids, String yo) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return recetas.buscarPorIds(ids, yo).stream()
                .collect(Collectors.toMap(RecetaResumenDto::id, Function.identity()));
    }

    /** Comprueba que el usuario puede ver la receta (404 si no existe o no es visible para él, D-032). */
    @Transactional(readOnly = true)
    public void exigirVisible(int id, String yo) {
        visible(id, yo);
    }

    /** Igual, y con {@code admin} también las ocultas por la administración (D-063), como el detalle. */
    @Transactional(readOnly = true)
    public void exigirVisible(int id, String yo, boolean admin) {
        visible(id, yo, admin);
    }

    @Transactional(readOnly = true)
    public List<RecetaResumenDto> mejorValoradas(String yo, int limite) {
        return recetas.buscar(FiltroRecetas.todasPublicadas(), yo, OrdenRecetas.MAS_VALORADAS, 0, limite);
    }

    @Transactional(readOnly = true)
    public List<RecetaResumenDto> ultimas(String yo, int limite) {
        return recetas.buscar(FiltroRecetas.todasPublicadas(), yo, OrdenRecetas.RECIENTES, 0, limite);
    }

    @Transactional(readOnly = true)
    public List<SugerenciaRecetaDto> sugerencias(String texto) {
        String contiene = Texto.patronContiene(texto);
        if (contiene == null) {
            return List.of();
        }
        return recetas.sugerencias(Texto.patronEmpiezaPor(texto), contiene, MAXIMO_SUGERENCIAS);
    }

    @Transactional(readOnly = true)
    public RecetaDetalleDto detalle(int id, String yo) {
        return detalle(id, yo, false);
    }

    /** Con {@code admin}, también las ocultas por la administración (D-063). */
    @Transactional(readOnly = true)
    public RecetaDetalleDto detalle(int id, String yo, boolean admin) {
        RecetaBase receta = visible(id, yo, admin);
        List<IngredienteRecetaDto> ingredientes = recetas.ingredientesDe(id);
        List<GrupoDto> grupos = ingredientes.stream()
                .map(IngredienteRecetaDto::grupo)
                .distinct()
                .sorted(Comparator.comparing(GrupoDto::nombre))
                .toList();
        return new RecetaDetalleDto(
                receta.id(),
                receta.titulo(),
                receta.instrucciones(),
                receta.autor(),
                receta.tipo(),
                ImagenService.url(receta.imagen()),
                fotosActuales(receta).stream().map(FotoReceta::dto).toList(),
                recetas.videoDe(receta.id()),
                receta.tiempo(),
                receta.comensales(),
                receta.dificultad(),
                receta.likes(),
                receta.meGusta(),
                receta.nota(),
                receta.valoraciones(),
                receta.publicada(),
                receta.fechaCreacion(),
                ingredientes,
                grupos,
                maxSemanal(grupos),
                receta.esDe(yo),
                receta.oculta(),
                receta.motivoOculta(),
                valoraciones.deUsuario(id, yo).orElse(null));
    }

    /** Crea la receta sin publicar (6.6). Las fotos son opcionales: hasta 6, la primera es la principal (D-083). */
    @Transactional
    public RecetaDetalleDto crear(RecetaRequest datos, List<MultipartFile> ficheros, String yo) {
        // Antes que nada: así no se procesan fotos que no se van a guardar
        if (recetas.contarDelAutor(yo) >= MAXIMO_RECETAS) {
            throw ExcepcionApi.conflicto("LIMITE_RECETAS", DEMASIADAS_RECETAS, null);
        }
        DatosReceta receta = validar(datos);
        List<FotoReceta> fotos = galeria(datos.fotos(), ficheros, List.of());
        recetas.insertar(
                receta.titulo(),
                receta.instrucciones(),
                yo,
                receta.tipo(),
                principal(fotos),
                receta.tiempo(),
                receta.comensales(),
                receta.dificultad());
        int id = recetas.ultimoIdInsertado();
        recetas.insertarIngredientes(id, receta.ingredientes());
        if (!fotos.isEmpty()) {
            recetas.insertarFotos(id, fotos);
        }
        if (receta.video() != null) {
            recetas.guardarVideo(id, receta.video());
        }
        return detalle(id, yo);
    }

    /**
     * Solo el autor (6.6). La galería queda como dice {@code fotos}: las que ya tenía y no siguen se borran tras el
     * commit; sin {@code fotos}, se conserva la que tenga (D-083).
     */
    @Transactional
    public RecetaDetalleDto editar(int id, RecetaRequest datos, List<MultipartFile> ficheros, String yo) {
        RecetaBase actual = delAutor(id, yo);
        DatosReceta receta = validar(datos);
        List<FotoReceta> antes = fotosActuales(actual);
        List<FotoReceta> fotos = galeria(datos.fotos(), ficheros, antes);
        recetas.actualizar(
                id,
                receta.titulo(),
                receta.instrucciones(),
                receta.tipo(),
                principal(fotos),
                receta.tiempo(),
                receta.comensales(),
                receta.dificultad());
        recetas.borrarIngredientes(id);
        recetas.insertarIngredientes(id, receta.ingredientes());
        recetas.borrarFotos(id);
        if (!fotos.isEmpty()) {
            recetas.insertarFotos(id, fotos);
        }
        recetas.guardarVideo(id, receta.video());
        Set<String> siguen = fotos.stream().map(FotoReceta::nombre).collect(Collectors.toSet());
        antes.stream()
                .map(FotoReceta::nombre)
                .filter(nombre -> !siguen.contains(nombre))
                .forEach(imagenes::borrarTrasCommit);
        return detalle(id, yo);
    }

    @Transactional
    public RecetaDetalleDto publicar(int id, boolean publicada, String yo) {
        RecetaBase receta = delAutor(id, yo);
        if (publicada && receta.oculta()) {
            throw ExcepcionApi.conflicto("RECETA_OCULTA", RECETA_OCULTA, null);
        }
        recetas.actualizarPublicada(id, publicada);
        return detalle(id, yo);
    }

    /**
     * Solo el autor. Si nadie la ha consumido se borra del todo; si no, se marca como eliminada para que esos consumos
     * sigan contando en las raciones semanales y en el histórico (D-061). En los dos casos se borra la imagen.
     */
    @Transactional
    public void eliminar(int id, String yo) {
        // Primero el bloqueo: un consumo que se registre a la vez termina antes o encuentra la receta ya eliminada
        recetas.bloquear(id);
        RecetaBase receta = delAutor(id, yo);
        List<FotoReceta> fotos = fotosActuales(receta);
        if (recetas.tieneConsumos(id)) {
            recetas.marcarEliminada(id);
            recetas.borrarLikes(id);
            recetas.borrarFotos(id);
        } else {
            recetas.borrar(id);
        }
        fotos.forEach(foto -> imagenes.borrarTrasCommit(foto.nombre()));
    }

    /** Idempotente; solo en recetas publicadas (D-033). */
    @Transactional
    public LikesDto darLike(int id, String yo) {
        RecetaBase receta = visible(id, yo);
        if (!receta.publica()) {
            throw ExcepcionApi.peticionIncorrecta(
                    "RECETA_NO_PUBLICADA", "Solo se puede dar «me gusta» a recetas publicadas.", null);
        }
        recetas.darLike(yo, id, reloj.instant());
        return new LikesDto(recetas.contarLikes(id), true);
    }

    @Transactional
    public LikesDto quitarLike(int id, String yo) {
        visible(id, yo);
        recetas.quitarLike(yo, id);
        return new LikesDto(recetas.contarLikes(id), false);
    }

    /**
     * Moderación (D-063): oculta la receta con un motivo, que verá su autor, o la vuelve a mostrar. Quien llama ya ha
     * comprobado que {@code yo} es administrador. Solo se ocultan recetas que el administrador puede ver: publicadas,
     * ya ocultas o suyas.
     */
    @Transactional
    public RecetaDetalleDto ocultar(int id, boolean oculta, String motivo, String yo) {
        visible(id, yo, true);
        String motivoLimpio = Texto.opcional(motivo);
        if (oculta && motivoLimpio == null) {
            throw ExcepcionApi.peticionIncorrecta(
                    "VALIDACION", "Revisa los datos del formulario.", Map.of("motivo", MOTIVO_OBLIGATORIO));
        }
        recetas.actualizarOculta(id, oculta, oculta ? motivoLimpio : null);
        return detalle(id, yo, true);
    }

    /** Las ocultas por la administración, de la más reciente a la más antigua (D-063). */
    @Transactional(readOnly = true)
    public Pagina<RecetaResumenDto> ocultas(String yo, int pagina, int tamano) {
        return buscarPagina(FiltroRecetas.ocultas(), yo, OrdenRecetas.RECIENTES, pagina, tamano);
    }

    /**
     * Veces a la semana que se puede comer una receta (6.1): el mínimo de las raciones semanales de sus grupos, sin
     * contar los que no tienen límite. {@code null} = sin límite.
     */
    static Integer maxSemanal(Collection<GrupoDto> grupos) {
        return grupos.stream()
                .map(GrupoDto::racionesSemana)
                .filter(Objects::nonNull)
                .min(Integer::compare)
                .orElse(null);
    }

    private RecetaBase visible(int id, String yo) {
        return visible(id, yo, false);
    }

    /** La receta, si el usuario la puede ver (D-032); si no, 404. También la usan las valoraciones (D-077). */
    RecetaBase visible(int id, String yo, boolean admin) {
        return recetas.buscarBase(id, yo)
                .filter(receta -> receta.visiblePara(yo, admin))
                .orElseThrow(() -> ExcepcionApi.noEncontrado("La receta no existe."));
    }

    private RecetaBase delAutor(int id, String yo) {
        RecetaBase receta = visible(id, yo);
        if (!receta.esDe(yo)) {
            throw ExcepcionApi.prohibido("Solo el autor puede modificar esta receta.");
        }
        return receta;
    }

    /** Datos ya comprobados y normalizados, listos para guardar. */
    private record DatosReceta(
            String titulo,
            String instrucciones,
            int tipo,
            int tiempo,
            int comensales,
            String dificultad,
            List<IngredienteCantidad> ingredientes,
            @Nullable String video) {}

    private DatosReceta validar(RecetaRequest datos) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (!catalogo.existeTipoReceta(datos.tipo())) {
            errores.put("tipo", TIPO_INEXISTENTE);
        }

        List<IngredienteCantidad> ingredientes = datos.ingredientes().stream()
                .map(i -> new IngredienteCantidad(i.ingrediente(), i.cantidad().strip()))
                .toList();
        Set<Integer> ids = new HashSet<>();
        ingredientes.forEach(i -> ids.add(i.ingrediente()));
        if (ids.size() != ingredientes.size()) {
            errores.put("ingredientes", INGREDIENTES_REPETIDOS);
        } else if (recetas.contarIngredientesExistentes(ids) != ids.size()) {
            errores.put("ingredientes", INGREDIENTE_INEXISTENTE);
        }

        String instrucciones = saneador.sanear(datos.instrucciones());
        if (!SaneadorHtml.tieneTexto(instrucciones)) {
            errores.put("instrucciones", INSTRUCCIONES_VACIAS);
        } else if (instrucciones.length() > LONGITUD_MAXIMA_HTML) {
            errores.put("instrucciones", "Las instrucciones son demasiado largas");
        }

        String video = sinVacio(datos.video());
        if (video != null && !esEnlaceDeVideo(video)) {
            errores.put("video", VIDEO_NO_VALIDO);
        }

        if (!errores.isEmpty()) {
            throw ExcepcionApi.peticionIncorrecta("VALIDACION", "Revisa los datos del formulario.", errores);
        }
        return new DatosReceta(
                datos.titulo().strip(),
                instrucciones,
                datos.tipo(),
                datos.tiempo(),
                datos.comensales(),
                datos.dificultad(),
                ingredientes,
                video);
    }

    /**
     * La galería de la receta. Una receta con imagen y sin filas en {@code foto_receta} (no debería haberla, V15) tiene
     * esa imagen como única foto.
     */
    private List<FotoReceta> fotosActuales(RecetaBase receta) {
        List<FotoReceta> fotos = recetas.fotosDe(receta.id());
        if (fotos.isEmpty() && receta.imagen() != null) {
            return List.of(new FotoReceta(receta.imagen(), 0, null));
        }
        return fotos;
    }

    /**
     * La galería que pide la receta, ya validada, con los ficheros nuevos guardados (se borran si la transacción se
     * deshace). Cada foto es una de {@code actuales} o un fichero nuevo, una sola vez, y no puede sobrar ningún
     * fichero. Sin {@code pedidas}, se quedan las actuales (D-083).
     */
    List<FotoReceta> galeria(
            @Nullable List<FotoRequest> pedidas, @Nullable List<MultipartFile> ficheros, List<FotoReceta> actuales) {
        List<MultipartFile> subidos = ficheros == null ? List.of() : ficheros;
        if (pedidas == null) {
            if (!subidos.isEmpty()) {
                throw errorFotos(Map.of("fotos", FICHEROS_SOBRANTES));
            }
            return actuales;
        }
        Set<String> deLaReceta = actuales.stream().map(FotoReceta::nombre).collect(Collectors.toSet());
        Set<String> existentes = new HashSet<>();
        Set<Integer> nuevas = new HashSet<>();
        Map<String, String> errores = new LinkedHashMap<>();
        for (int i = 0; i < pedidas.size(); i++) {
            FotoRequest foto = pedidas.get(i);
            String campo = "fotos[" + i + "]";
            if ((foto.existente() == null) == (foto.nueva() == null)) {
                errores.put(campo, FOTO_SIN_ORIGEN);
            } else if (foto.existente() != null) {
                if (!deLaReceta.contains(foto.existente())) {
                    errores.put(campo + ".existente", FOTO_AJENA);
                } else if (!existentes.add(foto.existente())) {
                    errores.put(campo + ".existente", FOTO_REPETIDA);
                }
            } else if (foto.nueva() >= subidos.size()
                    || subidos.get(foto.nueva()).isEmpty()) {
                errores.put(campo + ".nueva", FICHERO_QUE_FALTA);
            } else if (!nuevas.add(foto.nueva())) {
                errores.put(campo + ".nueva", FOTO_REPETIDA);
            }
        }
        if (errores.isEmpty() && nuevas.size() < subidos.size()) {
            errores.put("fotos", FICHEROS_SOBRANTES);
        }
        if (!errores.isEmpty()) {
            throw errorFotos(errores);
        }
        List<FotoReceta> fotos = new ArrayList<>();
        for (int i = 0; i < pedidas.size(); i++) {
            FotoRequest foto = pedidas.get(i);
            String nombre = foto.existente() != null
                    ? foto.existente()
                    : imagenes.guardar(subidos.get(foto.nueva()), "fotos[" + i + "]");
            fotos.add(new FotoReceta(nombre, i, sinVacio(foto.descripcion())));
        }
        return fotos;
    }

    /** ¿Es un enlace {@code https} a YouTube o Vimeo? Solo se enlaza, nunca se incrusta (D-083). */
    static boolean esEnlaceDeVideo(String enlace) {
        try {
            URI uri = new URI(enlace);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && uri.getUserInfo() == null
                    && SERVIDORES_VIDEO.contains(uri.getHost().toLowerCase(Locale.ROOT));
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static @Nullable String principal(List<FotoReceta> fotos) {
        return fotos.isEmpty() ? null : fotos.getFirst().nombre();
    }

    private static @Nullable String sinVacio(@Nullable String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    private static ExcepcionApi errorFotos(Map<String, String> errores) {
        return ExcepcionApi.peticionIncorrecta("VALIDACION", "Revisa las fotos.", errores);
    }
}
