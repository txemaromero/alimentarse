package es.alimentarse.support;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** {@code JavaMailSender} de los tests: guarda los correos en memoria en lugar de enviarlos. */
public class BuzonDePrueba extends JavaMailSenderImpl {

    private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(5);

    private final List<MimeMessage> enviados = new CopyOnWriteArrayList<>();

    @Override
    protected void doSend(MimeMessage[] mensajes, Object[] originales) {
        enviados.addAll(Arrays.asList(mensajes));
    }

    public List<MimeMessage> enviados() {
        return List.copyOf(enviados);
    }

    public void vaciar() {
        enviados.clear();
    }

    /** Espera a que llegue un correo para ese destinatario (los correos se envían en segundo plano). */
    public MimeMessage esperarCorreoPara(String email) {
        return esperarCorreoPara(email, "");
    }

    /** Como {@link #esperarCorreoPara(String)}, pero solo un correo cuyo asunto contenga el texto indicado. */
    public MimeMessage esperarCorreoPara(String email, String asuntoContiene) {
        long limite = System.nanoTime() + ESPERA_MAXIMA.toNanos();
        while (System.nanoTime() < limite) {
            Optional<MimeMessage> mensaje = correoPara(email, asuntoContiene);
            if (mensaje.isPresent()) {
                return mensaje.get();
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new AssertionError("No ha llegado ningún correo para " + email + " con asunto «" + asuntoContiene + "»");
    }

    public Optional<MimeMessage> correoPara(String email, String asuntoContiene) {
        return enviados.stream()
                .filter(mensaje -> destinatarios(mensaje).stream().anyMatch(email::equalsIgnoreCase))
                .filter(mensaje -> asunto(mensaje).contains(asuntoContiene))
                .findFirst();
    }

    private static String asunto(MimeMessage mensaje) {
        try {
            return mensaje.getSubject() == null ? "" : mensaje.getSubject();
        } catch (MessagingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String html(MimeMessage mensaje) {
        try {
            return (String) mensaje.getContent();
        } catch (IOException | MessagingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<String> destinatarios(MimeMessage mensaje) {
        try {
            Address[] direcciones = mensaje.getAllRecipients();
            return direcciones == null
                    ? List.of()
                    : Arrays.stream(direcciones).map(Address::toString).toList();
        } catch (MessagingException e) {
            throw new IllegalStateException(e);
        }
    }
}
