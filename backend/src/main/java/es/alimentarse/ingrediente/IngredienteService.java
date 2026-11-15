package es.alimentarse.ingrediente;

import es.alimentarse.catalogo.CatalogoMapper;
import es.alimentarse.comun.Pagina;
import es.alimentarse.comun.Texto;
import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.config.AppProperties;
import es.alimentarse.seguridad.LimiteIntentosService;
import es.alimentarse.seguridad.UsuarioAutenticado;
import es.alimentarse.usuario.Usuario;
import es.alimentarse.usuario.UsuarioMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ingredientes (6.8): cualquier usuario los consulta y los crea; solo un administrador los edita. Para crearlos hace
 * falta el email confirmado y hay un tope diario, salvo para los administradores: el catálogo es de todos (D-089).
 */
@Service
public class IngredienteService {

    static final int MAXIMO_SUGERENCIAS = 10;
    static final String NOMBRE_EN_USO = "Ya existe un ingrediente con ese nombre";
    static final String GRUPO_INEXISTENTE = "El grupo elegido no existe";
    static final String EMAIL_SIN_CONFIRMAR = "Para crear ingredientes, confirma antes tu email con el enlace que te"
            + " enviamos. Si no lo encuentras, pide otro desde «Mi perfil».";

    /** Ingredientes nuevos por usuario en un día, como mucho (D-089). */
    static final int NUEVOS_AL_DIA = 30;

    static final AppProperties.Limite LIMITE_NUEVOS = new AppProperties.Limite(NUEVOS_AL_DIA, Duration.ofDays(1));
    static final String DEMASIADOS_NUEVOS =
            "Has llegado al máximo de 30 ingredientes nuevos en un día. Podrás crear más dentro de un rato.";

    private final IngredienteMapper ingredientes;
    private final CatalogoMapper catalogo;
    private final UsuarioMapper usuarios;
    private final LimiteIntentosService limite;

    public IngredienteService(
            IngredienteMapper ingredientes,
            CatalogoMapper catalogo,
            UsuarioMapper usuarios,
            LimiteIntentosService limite) {
        this.ingredientes = ingredientes;
        this.catalogo = catalogo;
        this.usuarios = usuarios;
        this.limite = limite;
    }

    /**
     * Página de ingredientes ordenados por nombre; {@code nombre} busca «contiene» y {@code grupo} es opcional. Con
     * {@code sinAlimento}, solo los que no tienen alimento de BEDCA (D-080).
     */
    @Transactional(readOnly = true)
    public Pagina<IngredienteDto> listar(String nombre, Integer grupo, boolean sinAlimento, int pagina, int tamano) {
        String patron = Texto.patronContiene(nombre);
        long total = ingredientes.contar(patron, grupo, sinAlimento);
        long desplazamiento = Pagina.desplazamiento(pagina, tamano);
        List<IngredienteDto> contenido = desplazamiento >= total
                ? List.of()
                : ingredientes.buscar(patron, grupo, sinAlimento, desplazamiento, tamano);
        return Pagina.de(contenido, pagina, tamano, total);
    }

    /** Autocompletado: hasta 10, primero los que empiezan por el texto. Sin texto no devuelve nada. */
    @Transactional(readOnly = true)
    public List<IngredienteDto> sugerencias(String texto) {
        String contiene = Texto.patronContiene(texto);
        if (contiene == null) {
            return List.of();
        }
        return ingredientes.sugerencias(Texto.patronEmpiezaPor(texto), contiene, MAXIMO_SUGERENCIAS);
    }

    @Transactional
    public IngredienteDto crear(IngredienteRequest datos, UsuarioAutenticado yo) {
        if (!yo.esAdmin()
                && !usuarios.buscarPorId(yo.id()).map(Usuario::emailVerificado).orElse(false)) {
            throw ExcepcionApi.prohibido("EMAIL_SIN_CONFIRMAR", EMAIL_SIN_CONFIRMAR);
        }
        String nombre = datos.nombre().strip();
        validar(nombre, datos.grupo(), null);
        if (!yo.esAdmin()) {
            contarNuevo(yo.id());
        }
        ingredientes.insertar(nombre, datos.grupo());
        return buscar(ingredientes.ultimoIdInsertado());
    }

    @Transactional
    public IngredienteDto editar(int id, IngredienteRequest datos) {
        buscar(id);
        String nombre = datos.nombre().strip();
        validar(nombre, datos.grupo(), id);
        ingredientes.actualizar(id, nombre, datos.grupo());
        return buscar(id);
    }

    private void validar(String nombre, int grupo, Integer exceptoId) {
        if (!catalogo.existeGrupo(grupo)) {
            throw ExcepcionApi.peticionIncorrecta(
                    "VALIDACION", "Revisa los datos del formulario.", Map.of("grupo", GRUPO_INEXISTENTE));
        }
        if (ingredientes.existeNombre(nombre, exceptoId)) {
            throw ExcepcionApi.conflicto(NOMBRE_EN_USO + ".", Map.of("nombre", NOMBRE_EN_USO));
        }
    }

    private IngredienteDto buscar(int id) {
        return ingredientes.buscarPorId(id).orElseThrow(() -> ExcepcionApi.noEncontrado("El ingrediente no existe."));
    }

    /**
     * Cuenta el ingrediente en el tope diario. Va dentro de la transacción del alta: si el alta falla, no cuenta. Las
     * ventanas son las del límite de intentos (D-071), con la clave del usuario.
     */
    private void contarNuevo(String usuario) {
        try {
            limite.registrarIntento("ingrediente:" + usuario, LIMITE_NUEVOS);
        } catch (ExcepcionApi e) {
            throw ExcepcionApi.limiteAlcanzado("LIMITE_INGREDIENTES", DEMASIADOS_NUEVOS, e.getReintentarTras());
        }
    }
}
