package es.alimentarse.comun.error;

import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpStatus;

/** Error de negocio que se devuelve al cliente tal cual, con su estado HTTP y su código. */
public class ExcepcionApi extends RuntimeException {

    private final HttpStatus estado;
    private final String codigo;
    private final Map<String, String> errores;
    private final Duration reintentarTras;

    private ExcepcionApi(
            HttpStatus estado, String codigo, String mensaje, Map<String, String> errores, Duration reintentarTras) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
        this.errores = errores == null ? Map.of() : Map.copyOf(errores);
        this.reintentarTras = reintentarTras;
    }

    public static ExcepcionApi noEncontrado(String mensaje) {
        return new ExcepcionApi(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", mensaje, null, null);
    }

    /** 404 con un código propio, para lo que la interfaz trata de forma específica. */
    public static ExcepcionApi noEncontrado(String codigo, String mensaje) {
        return new ExcepcionApi(HttpStatus.NOT_FOUND, codigo, mensaje, null, null);
    }

    public static ExcepcionApi conflicto(String mensaje, Map<String, String> errores) {
        return new ExcepcionApi(HttpStatus.CONFLICT, "CONFLICTO", mensaje, errores, null);
    }

    /** 409 con un código propio, para conflictos que la interfaz trata de forma específica. */
    public static ExcepcionApi conflicto(String codigo, String mensaje, Map<String, String> errores) {
        return new ExcepcionApi(HttpStatus.CONFLICT, codigo, mensaje, errores, null);
    }

    public static ExcepcionApi peticionIncorrecta(String codigo, String mensaje, Map<String, String> errores) {
        return new ExcepcionApi(HttpStatus.BAD_REQUEST, codigo, mensaje, errores, null);
    }

    public static ExcepcionApi noAutorizado(String codigo, String mensaje) {
        return new ExcepcionApi(HttpStatus.UNAUTHORIZED, codigo, mensaje, null, null);
    }

    public static ExcepcionApi prohibido(String mensaje) {
        return prohibido("ACCESO_DENEGADO", mensaje);
    }

    public static ExcepcionApi prohibido(String codigo, String mensaje) {
        return new ExcepcionApi(HttpStatus.FORBIDDEN, codigo, mensaje, null, null);
    }

    /** 429 por un límite de uso, con su propio código y mensaje (D-089); {@code reintentarTras} puede ser null. */
    public static ExcepcionApi limiteAlcanzado(String codigo, String mensaje, Duration reintentarTras) {
        return new ExcepcionApi(HttpStatus.TOO_MANY_REQUESTS, codigo, mensaje, null, reintentarTras);
    }

    public static ExcepcionApi demasiadosIntentos(Duration reintentarTras) {
        return new ExcepcionApi(
                HttpStatus.TOO_MANY_REQUESTS,
                "DEMASIADOS_INTENTOS",
                "Has hecho demasiados intentos. Espera un poco antes de volver a probar.",
                null,
                reintentarTras);
    }

    public HttpStatus getEstado() {
        return estado;
    }

    public String getCodigo() {
        return codigo;
    }

    public Map<String, String> getErrores() {
        return errores;
    }

    /** Tiempo que el cliente debe esperar antes de reintentar (cabecera Retry-After), o {@code null}. */
    public Duration getReintentarTras() {
        return reintentarTras;
    }

    /** Valor de la cabecera Retry-After: segundos de espera redondeados hacia arriba (mínimo 1), o {@code null}. */
    public String segundosParaReintentar() {
        if (reintentarTras == null) {
            return null;
        }
        long segundos = (reintentarTras.toMillis() + 999) / 1000;
        return String.valueOf(Math.max(1, segundos));
    }

    public ErrorRespuesta comoRespuesta() {
        return new ErrorRespuesta(codigo, getMessage(), errores);
    }
}
