package es.alimentarse.admin;

import es.alimentarse.alimentacion.NutricionService;
import es.alimentarse.comun.Texto;
import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.ingrediente.IngredienteDto;
import es.alimentarse.ingrediente.IngredienteMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grupos de alimentos y limpieza de ingredientes desde la administración (D-065). Los cambios en las raciones se notan
 * al momento en los indicadores y en la recomendación de todos los usuarios, porque se calculan al pedirlos.
 */
@Service
public class CatalogoAdminService {

    static final String NOMBRE_GRUPO_EN_USO = "Ya existe un grupo con ese nombre";
    static final String GRUPO_EN_USO = "Hay ingredientes de este grupo: cámbialos de grupo antes de borrarlo.";
    static final String MISMO_INGREDIENTE = "Elige un ingrediente distinto";
    static final String INGREDIENTE_EN_USO =
            "Lo usa alguna receta: fusiónalo con otro ingrediente en lugar de borrarlo.";
    static final String ALIMENTO_INEXISTENTE = "Ese alimento no está en BEDCA";
    static final String PIEZA_REPETIDA = "Esta pieza ya está";
    static final String PIEZA_GENERAL = "Esta unidad ya se convierte sola";

    /** Alimentos que devuelve como mucho la búsqueda en BEDCA, y palabras que tiene en cuenta. */
    static final int MAXIMO_ALIMENTOS = 10;

    static final int MAXIMO_PALABRAS = 6;

    private final CatalogoAdminMapper catalogo;
    private final IngredienteMapper ingredientes;
    private final AdminService admin;

    public CatalogoAdminService(CatalogoAdminMapper catalogo, IngredienteMapper ingredientes, AdminService admin) {
        this.catalogo = catalogo;
        this.ingredientes = ingredientes;
        this.admin = admin;
    }

    @Transactional(readOnly = true)
    public List<GrupoAdminDto> grupos(String yo) {
        admin.exigirAdministrador(yo);
        return catalogo.grupos();
    }

    @Transactional
    public GrupoAdminDto crearGrupo(GrupoRequest datos, String yo) {
        admin.exigirAdministrador(yo);
        String nombre = nombreLibre(datos.nombre(), null);
        catalogo.insertarGrupo(nombre, datos.racionesSemana(), datos.basico());
        return grupo(catalogo.ultimoIdInsertado());
    }

    @Transactional
    public GrupoAdminDto editarGrupo(int id, GrupoRequest datos, String yo) {
        admin.exigirAdministrador(yo);
        grupo(id);
        String nombre = nombreLibre(datos.nombre(), id);
        catalogo.actualizarGrupo(id, nombre, datos.racionesSemana(), datos.basico());
        return grupo(id);
    }

    /** Solo si ningún ingrediente lo usa (409 {@code GRUPO_EN_USO}). */
    @Transactional
    public void borrarGrupo(int id, String yo) {
        admin.exigirAdministrador(yo);
        catalogo.bloquearGrupo(id);
        grupo(id);
        if (catalogo.contarIngredientesDeGrupo(id) > 0) {
            throw ExcepcionApi.conflicto("GRUPO_EN_USO", GRUPO_EN_USO, null);
        }
        catalogo.borrarGrupo(id);
    }

    /**
     * Fusiona un ingrediente duplicado ({@code origen}) con otro ({@code destino}) y borra el origen. Las recetas que
     * llevaban el origen pasan a llevar el destino; si ya llevaban los dos, sus cantidades se suman en el texto. Si los
     * grupos son distintos, esas recetas pasan a contar en el grupo del destino, también en los consumos ya
     * registrados.
     */
    @Transactional
    public FusionDto fusionarIngredientes(int origen, int destino, String yo) {
        admin.exigirAdministrador(yo);
        if (origen == destino) {
            throw ExcepcionApi.peticionIncorrecta(
                    "VALIDACION", "Revisa los datos del formulario.", Map.of("destino", MISMO_INGREDIENTE));
        }
        catalogo.bloquearIngredientes(origen, destino);
        ingrediente(origen);
        ingrediente(destino);
        int recetas = catalogo.contarRecetasConIngrediente(origen);
        catalogo.sumarCantidades(origen, destino);
        catalogo.borrarLineasRepetidas(origen, destino);
        catalogo.moverLineas(origen, destino);
        catalogo.heredarAlimento(origen, destino);
        catalogo.heredarPiezas(origen, destino);
        catalogo.borrarIngrediente(origen);
        return new FusionDto(ingrediente(destino), recetas);
    }

    /** Solo si ninguna receta lo usa (409 {@code INGREDIENTE_EN_USO}): para limpiar ingredientes creados por error. */
    @Transactional
    public void borrarIngrediente(int id, String yo) {
        admin.exigirAdministrador(yo);
        catalogo.bloquearIngredientes(id, id);
        ingrediente(id);
        if (catalogo.contarRecetasConIngrediente(id) > 0) {
            throw ExcepcionApi.conflicto("INGREDIENTE_EN_USO", INGREDIENTE_EN_USO, null);
        }
        catalogo.borrarIngrediente(id);
    }

    /**
     * Alimentos de BEDCA para asignar a un ingrediente (D-080): los que contienen todas las palabras del texto, en
     * cualquier orden («pollo crudo» encuentra «Pollo, entero, con piel, crudo»). Sin texto no devuelve nada.
     */
    @Transactional(readOnly = true)
    public List<AlimentoBedcaDto> buscarAlimentos(String texto, String yo) {
        admin.exigirAdministrador(yo);
        List<String> palabras = Arrays.stream(texto.strip().split("[\\s,]+"))
                .map(Texto::patronContiene)
                .filter(Objects::nonNull)
                .limit(MAXIMO_PALABRAS)
                .toList();
        if (palabras.isEmpty()) {
            return List.of();
        }
        return catalogo.buscarAlimentos(palabras, Texto.patronEmpiezaPor(texto.strip()), MAXIMO_ALIMENTOS).stream()
                .map(CatalogoAdminService::dto)
                .toList();
    }

    /** Los datos nutricionales de un ingrediente para editarlos: su alimento de BEDCA y sus piezas (D-080, D-081). */
    @Transactional(readOnly = true)
    public NutricionIngredienteDto nutricion(int ingrediente, String yo) {
        admin.exigirAdministrador(yo);
        ingrediente(ingrediente);
        return new NutricionIngredienteDto(
                catalogo.alimentoDe(ingrediente).map(CatalogoAdminService::dto).orElse(null),
                catalogo.piezasDe(ingrediente));
    }

    /**
     * Sustituye los datos nutricionales de un ingrediente: el alimento de BEDCA con el que se calcula su información
     * nutricional ({@code null}, ninguno) y los pesos de sus piezas (D-080, D-081). Empieza bloqueando el ingrediente,
     * como al fusionarlo o borrarlo (D-061).
     */
    @Transactional
    public IngredienteDto guardarNutricion(int ingrediente, NutricionIngredienteRequest datos, String yo) {
        admin.exigirAdministrador(yo);
        catalogo.bloquearIngredientes(ingrediente, ingrediente);
        ingrediente(ingrediente);
        Integer alimento = datos.alimento();
        Map<String, String> errores = new LinkedHashMap<>();
        if (alimento != null && !catalogo.existeAlimento(alimento)) {
            errores.put("alimento", ALIMENTO_INEXISTENTE);
        }
        List<PiezaDto> piezas = piezasValidas(datos.piezas(), errores);
        if (!errores.isEmpty()) {
            throw ExcepcionApi.peticionIncorrecta("VALIDACION", "Revisa los datos del formulario.", errores);
        }
        if (alimento == null) {
            catalogo.quitarAlimento(ingrediente);
        } else {
            catalogo.asignarAlimento(ingrediente, alimento);
        }
        catalogo.borrarPiezas(ingrediente);
        if (!piezas.isEmpty()) {
            catalogo.insertarPiezas(ingrediente, piezas);
        }
        return ingrediente(ingrediente);
    }

    /**
     * Las piezas en minúsculas y sin espacios. No puede haber dos que sean la misma en singular y en plural («lata» y
     * «latas»), ni una que ya se convierte sola («gramo», «cucharada»): no se usaría nunca.
     */
    static List<PiezaDto> piezasValidas(List<PiezaDto> piezas, Map<String, String> errores) {
        List<PiezaDto> validas = new ArrayList<>();
        for (int i = 0; i < piezas.size(); i++) {
            String pieza = piezas.get(i).pieza().strip().toLowerCase(Locale.ROOT);
            String campo = "piezas[" + i + "].pieza";
            if (NutricionService.esUnidadGeneral(pieza)) {
                errores.put(campo, PIEZA_GENERAL);
            } else if (validas.stream().anyMatch(otra -> NutricionService.mismaPieza(otra.pieza(), pieza))) {
                errores.put(campo, PIEZA_REPETIDA);
            }
            validas.add(new PiezaDto(pieza, piezas.get(i).gramos()));
        }
        return validas;
    }

    static AlimentoBedcaDto dto(AlimentoBedca alimento) {
        BigDecimal kj = alimento.energiaKj();
        return new AlimentoBedcaDto(
                alimento.id(),
                alimento.nombre(),
                kj == null ? null : kj.divide(NutricionService.KJ_POR_KCAL, 0, RoundingMode.HALF_UP),
                sinCeros(kj),
                sinCeros(alimento.grasas()),
                sinCeros(alimento.grasasSaturadas()),
                sinCeros(alimento.hidratos()),
                sinCeros(alimento.fibra()),
                sinCeros(alimento.proteinas()),
                sinCeros(alimento.sodioMg()));
    }

    /** El valor tal cual, sin los ceros de relleno de la columna: «71.500000» → «71.5», «100.000000» → «100». */
    static @Nullable BigDecimal sinCeros(@Nullable BigDecimal valor) {
        if (valor == null) {
            return null;
        }
        BigDecimal limpio = valor.stripTrailingZeros();
        return limpio.scale() < 0 ? limpio.setScale(0) : limpio;
    }

    private String nombreLibre(String nombre, Integer exceptoId) {
        String limpio = nombre.strip();
        if (catalogo.existeNombreGrupo(limpio, exceptoId)) {
            throw ExcepcionApi.conflicto(NOMBRE_GRUPO_EN_USO + ".", Map.of("nombre", NOMBRE_GRUPO_EN_USO));
        }
        return limpio;
    }

    private GrupoAdminDto grupo(int id) {
        return catalogo.grupoPorId(id).orElseThrow(() -> ExcepcionApi.noEncontrado("El grupo no existe."));
    }

    private IngredienteDto ingrediente(int id) {
        return ingredientes.buscarPorId(id).orElseThrow(() -> ExcepcionApi.noEncontrado("El ingrediente no existe."));
    }
}
