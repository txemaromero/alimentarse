package es.alimentarse.support;

import es.alimentarse.push.EnvioPush;
import es.alimentarse.push.SuscripcionPush;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Envío push en memoria para los tests de integración (D-078): guarda los avisos en lugar de mandarlos a un servicio
 * push, y cada endpoint puede responder lo que diga el test (por ejemplo, que la suscripción ya no existe).
 */
public class EnvioPushDePrueba implements EnvioPush {

    /** Un aviso que se habría enviado. */
    public record Aviso(String endpoint, String mensaje, Duration validez) {}

    private final List<Aviso> enviados = new CopyOnWriteArrayList<>();
    private final Map<String, Resultado> respuestas = new ConcurrentHashMap<>();

    @Override
    public Resultado enviar(SuscripcionPush suscripcion, String mensaje, Duration validez) {
        enviados.add(new Aviso(suscripcion.endpoint(), mensaje, validez));
        return respuestas.getOrDefault(suscripcion.endpoint(), Resultado.ENTREGADO);
    }

    /** Los avisos enviados a ese endpoint. */
    public List<Aviso> enviadosA(String endpoint) {
        return enviados.stream().filter(a -> a.endpoint().equals(endpoint)).toList();
    }

    /** Lo que responderá el servicio push a los avisos para ese endpoint. */
    public void responder(String endpoint, Resultado resultado) {
        respuestas.put(endpoint, resultado);
    }
}
