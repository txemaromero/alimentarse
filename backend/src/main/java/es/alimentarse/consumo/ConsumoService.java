package es.alimentarse.consumo;

import es.alimentarse.comun.Pagina;
import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.config.AppProperties;
import es.alimentarse.receta.RecetaService;
import es.alimentarse.seguridad.LimiteIntentosService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumos del usuario (6.7): registrar, consultar el histórico, cambiar la fecha y borrar. Cada usuario solo ve y
 * toca los suyos; los ajenos son un 404 (D-039).
 */
@Service
public class ConsumoService {

    /** Desfase de reloj del cliente que se tolera antes de considerar una fecha futura. */
    static final Duration MARGEN_RELOJ = Duration.ofMinutes(1);

    static final String FECHA_FUTURA = "La fecha no puede ser futura";

    /**
     * Consumos que un usuario registra en un día, como mucho (D-090). Nadie come tantas recetas: sin tope, un script
     * podía llenar la tabla y hacer lentas las cuentas de su semana.
     */
    static final int AL_DIA = 50;

    static final AppProperties.Limite LIMITE = new AppProperties.Limite(AL_DIA, Duration.ofDays(1));
    static final String DEMASIADOS =
            "Has llegado al máximo de 50 consumos registrados en un día. Podrás registrar más dentro de un rato.";

    private final ConsumoMapper consumos;
    private final RecetaService recetas;
    private final LimiteIntentosService limite;
    private final Clock reloj;

    public ConsumoService(ConsumoMapper consumos, RecetaService recetas, LimiteIntentosService limite, Clock reloj) {
        this.consumos = consumos;
        this.recetas = recetas;
        this.limite = limite;
        this.reloj = reloj;
    }

    /**
     * Solo se pueden registrar recetas que el usuario puede ver (publicadas o propias), y como mucho {@link #AL_DIA}
     * al día. También pasan por aquí las comidas del planificador que se marcan como comidas.
     */
    @Transactional
    public ConsumoDto registrar(ConsumoRequest datos, String yo) {
        recetas.exigirVisible(datos.receta(), yo);
        Instant fecha = fechaValida(datos.fecha() == null ? reloj.instant() : datos.fecha());
        contarNuevo(yo);
        consumos.insertar(yo, datos.receta(), fecha);
        return buscarDto(consumos.ultimoIdInsertado(), yo);
    }

    /**
     * Cuenta el consumo en el tope diario, como los ingredientes nuevos (D-089): con las ventanas del límite de intentos
     * (D-071) y dentro de la transacción del alta, así que si el alta falla, no cuenta.
     */
    private void contarNuevo(String yo) {
        try {
            limite.registrarIntento("consumo:" + yo, LIMITE);
        } catch (ExcepcionApi e) {
            throw ExcepcionApi.limiteAlcanzado("LIMITE_CONSUMOS", DEMASIADOS, e.getReintentarTras());
        }
    }

    @Transactional(readOnly = true)
    public Pagina<ConsumoDto> historial(String yo, int pagina, int tamano) {
        long total = consumos.contar(yo);
        long desplazamiento = Pagina.desplazamiento(pagina, tamano);
        List<ConsumoDto> contenido =
                desplazamiento >= total ? List.of() : consumos.historial(yo, desplazamiento, tamano);
        return Pagina.de(contenido, pagina, tamano, total);
    }

    @Transactional(readOnly = true)
    public List<ConsumoDto> ultimos(String yo, int limite) {
        return consumos.historial(yo, 0, limite);
    }

    @Transactional
    public ConsumoDto cambiarFecha(long id, FechaConsumoRequest datos, String yo) {
        propio(id, yo);
        consumos.actualizarFecha(id, fechaValida(datos.fecha()));
        return buscarDto(id, yo);
    }

    @Transactional
    public void borrar(long id, String yo) {
        propio(id, yo);
        consumos.borrar(id);
    }

    /**
     * Sin fracciones de segundo (la columna es {@code TIMESTAMP}). Una fecha futura dentro del margen de reloj se
     * guarda como «ahora»; más allá, es un error.
     */
    private Instant fechaValida(Instant fecha) {
        Instant ahora = reloj.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant truncada = fecha.truncatedTo(ChronoUnit.SECONDS);
        if (truncada.isAfter(ahora.plus(MARGEN_RELOJ))) {
            throw ExcepcionApi.peticionIncorrecta(
                    "VALIDACION", "Revisa los datos del formulario.", Map.of("fecha", FECHA_FUTURA));
        }
        return truncada.isAfter(ahora) ? ahora : truncada;
    }

    private Consumo propio(long id, String yo) {
        return consumos.buscarPorId(id).filter(c -> c.esDe(yo)).orElseThrow(ConsumoService::noEncontrado);
    }

    private ConsumoDto buscarDto(long id, String yo) {
        return consumos.buscarDto(id, yo).orElseThrow(ConsumoService::noEncontrado);
    }

    private static ExcepcionApi noEncontrado() {
        return ExcepcionApi.noEncontrado("El consumo no existe.");
    }
}
