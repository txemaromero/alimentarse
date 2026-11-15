package es.alimentarse.seguridad;

import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.config.AppProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Límite de intentos por clave (por ejemplo, {@code login:<ip>}) con ventana fija. Las ventanas están en la base de
 * datos, así que el límite vale aunque haya varios servidores (D-071).
 */
@Service
public class LimiteIntentosService {

    /** Longitud de la columna; una IP nunca llega, pero {@code X-Forwarded-For} lo escribe el cliente. */
    static final int LONGITUD_MAXIMA_CLAVE = 100;

    /** Cada cuántos intentos se borran las ventanas terminadas. */
    static final int INTENTOS_ENTRE_LIMPIEZAS = 1_000;

    private final LimiteIntentosMapper ventanas;
    private final Clock reloj;
    private final AtomicInteger intentosDesdeLimpieza = new AtomicInteger();

    public LimiteIntentosService(LimiteIntentosMapper ventanas, Clock reloj) {
        this.ventanas = ventanas;
        this.reloj = reloj;
    }

    /**
     * Cuenta un intento para la clave.
     *
     * @throws ExcepcionApi 429 si la clave ya ha agotado sus intentos en la ventana actual
     */
    public void registrarIntento(String clave, AppProperties.Limite limite) {
        String recortada = recortar(clave);
        Instant ahora = ahora();
        ventanas.registrar(recortada, ahora, ahora.plus(limite.ventana()));
        LimiteIntentosMapper.Ventana ventana = ventanas.ventana(recortada);
        if (intentosDesdeLimpieza.incrementAndGet() >= INTENTOS_ENTRE_LIMPIEZAS) {
            intentosDesdeLimpieza.set(0);
            ventanas.limpiar(ahora);
        }
        if (ventana.intentos() > limite.maximo()) {
            throw ExcepcionApi.demasiadosIntentos(Duration.between(ahora, ventana.fin()));
        }
    }

    /**
     * Para los límites que solo cuentan los fallos (D-090): comprueba la clave sin sumar nada.
     *
     * @return cuánto falta para que termine la ventana, si la clave ya tiene {@code maximo} fallos en ella
     */
    public Duration agotado(String clave, AppProperties.Limite limite) {
        Instant ahora = ahora();
        LimiteIntentosMapper.Ventana ventana = ventanas.ventana(recortar(clave));
        if (ventana == null || !ventana.fin().isAfter(ahora) || ventana.intentos() < limite.maximo()) {
            return Duration.ZERO;
        }
        return Duration.between(ahora, ventana.fin());
    }

    /**
     * Suma un fallo a la clave, en su propia transacción: la petición que falla deshace la suya, y el fallo tiene que
     * quedar contado (D-090).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFallo(String clave, AppProperties.Limite limite) {
        Instant ahora = ahora();
        ventanas.registrar(recortar(clave), ahora, ahora.plus(limite.ventana()));
    }

    /** Pone la clave a cero. */
    public void olvidar(String clave) {
        ventanas.borrar(recortar(clave));
    }

    private Instant ahora() {
        return reloj.instant().truncatedTo(ChronoUnit.SECONDS);
    }

    private static String recortar(String clave) {
        return clave.length() > LONGITUD_MAXIMA_CLAVE ? clave.substring(0, LONGITUD_MAXIMA_CLAVE) : clave;
    }
}
