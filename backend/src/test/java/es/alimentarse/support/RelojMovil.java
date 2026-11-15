package es.alimentarse.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Reloj de tests que solo avanza cuando se le pide. */
public class RelojMovil extends Clock {

    private Instant ahora;

    public RelojMovil(Instant inicio) {
        this.ahora = inicio;
    }

    public void avanzar(Duration duracion) {
        ahora = ahora.plus(duracion);
    }

    @Override
    public Instant instant() {
        return ahora;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return this;
    }
}
