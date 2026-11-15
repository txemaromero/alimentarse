package es.alimentarse.push;

import static java.nio.charset.StandardCharsets.UTF_8;

import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.config.PushProperties;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Avisos push (D-078): el recordatorio de la comida y la cena planificadas, una hora antes (configurable), en los
 * navegadores en los que el usuario los ha activado. El mensaje sigue el formato del service worker de Angular
 * ({@code notification}), que lo muestra y, al pulsarlo, abre la receta.
 */
@Service
public class PushService {

    static final String ICONO = "/icons/icono-192.png";
    static final String DESACTIVADOS = "Los avisos no están disponibles en este servidor.";
    static final String SERVICIO_NO_ADMITIDO = "Ese servicio de avisos no está admitido";
    static final ZoneId ZONA_POR_DEFECTO = ZoneId.of("Europe/Madrid");

    /** Navegadores con los avisos activados por usuario, como mucho (D-089): cada aviso se envía a todos. */
    static final int MAXIMO_NAVEGADORES = 10;

    static final String DEMASIADOS_NAVEGADORES =
            "Ya tienes los avisos activados en 10 navegadores: desactívalos en alguno para activarlos aquí.";

    /** Para la prueba: si el dispositivo está apagado, no tiene sentido entregarla mucho después. */
    static final Duration VALIDEZ_PRUEBA = Duration.ofMinutes(10);

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final Logger log = LoggerFactory.getLogger(PushService.class);

    private final PushProperties propiedades;
    private final PushMapper push;
    private final EnvioPush envio;
    private final JsonMapper json;
    private final Clock reloj;

    public PushService(PushProperties propiedades, PushMapper push, EnvioPush envio, JsonMapper json, Clock reloj) {
        this.propiedades = propiedades;
        this.push = push;
        this.envio = envio;
        this.json = json;
        this.reloj = reloj;
    }

    /** La clave pública VAPID, que el navegador necesita para suscribirse, o {@code null} si no hay avisos. */
    public ClavePushDto clave() {
        return new ClavePushDto(
                propiedades.activos() ? propiedades.clavePublica().strip() : null);
    }

    /** Guarda la suscripción de un navegador del usuario. Solo se admiten los servicios push conocidos. */
    @Transactional
    public void suscribir(String yo, SuscripcionPushRequest datos) {
        exigirActivos();
        Map<String, String> errores = new LinkedHashMap<>();
        if (!servicioAdmitido(datos.endpoint())) {
            errores.put("endpoint", SERVICIO_NO_ADMITIDO);
        }
        if (!clave(datos.p256dh(), 65)) {
            errores.put("p256dh", "La clave del navegador no es válida");
        }
        if (!clave(datos.auth(), 16)) {
            errores.put("auth", "El secreto del navegador no es válido");
        }
        ZoneId zona = ZONA_POR_DEFECTO;
        try {
            if (datos.zona() != null && !datos.zona().isBlank()) {
                zona = ZoneId.of(datos.zona().strip());
            }
        } catch (DateTimeException e) {
            errores.put("zona", "La zona horaria no es válida");
        }
        if (!errores.isEmpty()) {
            throw ExcepcionApi.peticionIncorrecta("VALIDACION", "Revisa los datos de la suscripción.", errores);
        }
        // Volver a suscribir este mismo navegador no cuenta: se actualiza
        if (push.contarOtros(yo, hash(datos.endpoint())) >= MAXIMO_NAVEGADORES) {
            throw ExcepcionApi.conflicto("LIMITE_NAVEGADORES", DEMASIADOS_NAVEGADORES, null);
        }
        push.guardar(
                yo,
                datos.endpoint().strip(),
                hash(datos.endpoint()),
                datos.p256dh().strip(),
                datos.auth().strip(),
                zona.getId(),
                reloj.instant().truncatedTo(ChronoUnit.SECONDS));
    }

    /** Idempotente. */
    @Transactional
    public void desuscribir(String yo, String endpoint) {
        push.borrar(yo, hash(endpoint));
    }

    /** Un aviso de prueba a cada navegador del usuario. Devuelve a cuántos ha llegado al servicio push. */
    public PruebaPushDto probar(String yo) {
        exigirActivos();
        List<SuscripcionPush> suscripciones = push.deUsuario(yo);
        if (suscripciones.isEmpty()) {
            throw ExcepcionApi.conflicto("SIN_SUSCRIPCIONES", "Activa antes los avisos en este dispositivo.", null);
        }
        String mensaje = mensaje(
                "Avisos de Toca",
                "Así te recordaremos la comida y la cena que tengas planificadas.",
                "/planificador",
                "prueba");
        int entregados = 0;
        for (SuscripcionPush suscripcion : suscripciones) {
            if (enviar(suscripcion, mensaje, VALIDEZ_PRUEBA)) {
                entregados++;
            }
        }
        return new PruebaPushDto(entregados);
    }

    /**
     * Cada pocos minutos: el recordatorio de las comidas que empiezan dentro de la antelación configurada, una sola vez
     * cada una. Si hay varios servidores, cada comida la avisa solo el que la reclama primero.
     */
    @Scheduled(fixedDelayString = "${app.push.intervalo:PT5M}", initialDelayString = "${app.push.intervalo:PT5M}")
    public void avisarComidas() {
        if (!propiedades.activos()) {
            return;
        }
        Instant ahora = reloj.instant();
        for (ComidaPorAvisar comida : push.comidasPorAvisar(ahora, ahora.plus(propiedades.antelacion()))) {
            if (push.reclamar(comida.id(), ahora.truncatedTo(ChronoUnit.SECONDS)) == 1) {
                Duration validez = Duration.between(ahora, comida.fecha());
                for (SuscripcionPush suscripcion : push.deUsuario(comida.usuario())) {
                    enviar(suscripcion, recordatorio(comida, suscripcion.zona(), ahora), validez);
                }
            }
        }
    }

    /** «Comida: Tortilla de patata», «Hoy a las 14:00, según tu planificador.», en la zona del navegador. */
    String recordatorio(ComidaPorAvisar comida, String zona, Instant ahora) {
        ZoneId zonaId = zonaOPorDefecto(zona);
        ZonedDateTime cuando = comida.fecha().atZone(zonaId);
        // Los mismos tramos que el planificador: antes de las 18:00, comida
        String momento = cuando.getHour() < 18 ? "Comida" : "Cena";
        String dia = cuando.toLocalDate().equals(ahora.atZone(zonaId).toLocalDate()) ? "Hoy" : "Mañana";
        return mensaje(
                momento + ": " + comida.titulo(),
                dia + " a las " + HORA.format(cuando) + ", según tu planificador.",
                "/recetas/" + comida.receta(),
                "comida-" + comida.id());
    }

    /** El JSON que entiende el service worker de Angular: al pulsar el aviso, abre (o enfoca) la aplicación en la URL. */
    private String mensaje(String titulo, String cuerpo, String url, String etiqueta) {
        Map<String, Object> aviso = new LinkedHashMap<>();
        aviso.put("title", titulo);
        aviso.put("body", cuerpo);
        aviso.put("icon", ICONO);
        aviso.put("lang", "es");
        aviso.put("tag", etiqueta);
        aviso.put(
                "data",
                Map.of(
                        "onActionClick",
                        Map.of("default", Map.of("operation", "navigateLastFocusedOrOpen", "url", url))));
        return json.writeValueAsString(Map.of("notification", aviso));
    }

    /**
     * Un navegador que falla no deja sin aviso a los demás: los recordatorios de todas las comidas salen en serie, y
     * una excepción cortaría el lote (D-089). La URL se vuelve a comprobar por si la lista de servicios ha cambiado
     * desde que se suscribió.
     */
    private boolean enviar(SuscripcionPush suscripcion, String mensaje, Duration validez) {
        if (!servicioAdmitido(suscripcion.endpoint())) {
            log.warn(
                    "La suscripción {} es de un servicio que ya no se admite: no se le envía el aviso",
                    suscripcion.id());
            return false;
        }
        EnvioPush.Resultado resultado;
        try {
            resultado = envio.enviar(suscripcion, mensaje, validez);
        } catch (RuntimeException e) {
            log.warn("No se ha podido enviar el aviso de la suscripción {}: {}", suscripcion.id(), e.toString());
            return false;
        }
        if (resultado == EnvioPush.Resultado.CADUCADA) {
            push.borrarPorId(suscripcion.id());
        }
        return resultado == EnvioPush.Resultado.ENTREGADO;
    }

    /**
     * Solo HTTPS, en el puerto estándar y a un dominio de la lista: el servidor envía peticiones a esa URL, y no debe
     * poder usarse para llegar a otras máquinas.
     */
    boolean servicioAdmitido(String endpoint) {
        try {
            URI uri = new URI(endpoint.strip());
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || host == null
                    || uri.getUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)) {
                return false;
            }
            String dominio = host.toLowerCase(Locale.ROOT);
            return propiedades.servicios().stream()
                    .map(servicio -> servicio.strip().toLowerCase(Locale.ROOT))
                    .anyMatch(servicio -> dominio.equals(servicio) || dominio.endsWith("." + servicio));
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private void exigirActivos() {
        if (!propiedades.activos()) {
            throw ExcepcionApi.conflicto("AVISOS_DESACTIVADOS", DESACTIVADOS, null);
        }
    }

    private static boolean clave(String texto, int longitud) {
        try {
            byte[] bytes = CifradoPush.base64Url(texto);
            return bytes.length == longitud && (longitud != 65 || CifradoPush.puntoP256(bytes));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static ZoneId zonaOPorDefecto(String zona) {
        try {
            return ZoneId.of(zona);
        } catch (DateTimeException | NullPointerException e) {
            return ZONA_POR_DEFECTO;
        }
    }

    /** SHA-256 del endpoint en hexadecimal: identifica al navegador en la base de datos. */
    static String hash(String endpoint) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256")
                            .digest(endpoint.strip().getBytes(UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
