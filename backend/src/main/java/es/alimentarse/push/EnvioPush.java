package es.alimentarse.push;

import java.time.Duration;

/** Envía un aviso a un navegador a través de su servicio push (D-078). Los tests lo sustituyen. */
public interface EnvioPush {

    enum Resultado {
        ENTREGADO,
        /** El servicio push ya no conoce la suscripción (404 o 410): hay que borrarla. */
        CADUCADA,
        ERROR
    }

    /**
     * @param mensaje el JSON del aviso, que se cifra para ese navegador
     * @param validez cuánto puede esperar el servicio push a entregarlo si el dispositivo está apagado
     */
    Resultado enviar(SuscripcionPush suscripcion, String mensaje, Duration validez);
}
