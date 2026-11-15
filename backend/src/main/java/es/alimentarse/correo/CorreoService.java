package es.alimentarse.correo;

import es.alimentarse.config.AppProperties;
import es.alimentarse.usuario.AvisoCuenta;
import es.alimentarse.usuario.ConfirmacionEmailSolicitada;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/** Correos de la aplicación, en HTML a partir de las plantillas de {@code mail/}. */
@Service
public class CorreoService {

    static final String REMITENTE = "Alimentarse";
    static final String ASUNTO_BIENVENIDA = "Te damos la bienvenida a Alimentarse";
    static final String ASUNTO_RECUPERACION = "Restablece tu contraseña de Alimentarse";
    static final String ASUNTO_BAJA = "Tu cuenta de Alimentarse se ha eliminado";
    static final String ASUNTO_CONFIRMAR_EMAIL = "Confirma tu email de Alimentarse";
    static final String ASUNTO_CONFIRMAR_EMAIL_NUEVO = "Confirma tu nuevo email de Alimentarse";
    static final String ASUNTO_EMAIL_CAMBIADO = "Ha cambiado el email de tu cuenta de Alimentarse";
    static final String ASUNTO_GOOGLE_VINCULADO = "Tu cuenta de Alimentarse se ha vinculado con Google";
    static final String ASUNTO_GOOGLE_DESVINCULADO = "Tu cuenta de Alimentarse ya no está vinculada con Google";
    private static final Locale ESPANOL = Locale.of("es", "ES");

    private final JavaMailSender mailSender;
    private final TemplateEngine plantillas;
    private final AppProperties propiedades;

    public CorreoService(JavaMailSender mailSender, TemplateEngine plantillasCorreo, AppProperties propiedades) {
        this.mailSender = mailSender;
        this.plantillas = plantillasCorreo;
        this.propiedades = propiedades;
    }

    /** Con {@code tokenConfirmacion}, lleva el enlace para confirmar el email (D-088). */
    public void enviarBienvenida(String email, String nombre, @Nullable String tokenConfirmacion) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("nombre", nombre);
        variables.put("urlLogin", propiedades.baseUrl() + "/login");
        variables.put("urlConfirmar", tokenConfirmacion == null ? null : urlConfirmar(tokenConfirmacion));
        enviar(email, ASUNTO_BIENVENIDA, "bienvenida", variables);
    }

    /** El enlace para confirmar el email de la cuenta o el nuevo (D-088). */
    public void enviarConfirmacionEmail(ConfirmacionEmailSolicitada confirmacion) {
        enviar(
                confirmacion.email(),
                confirmacion.cambio() ? ASUNTO_CONFIRMAR_EMAIL_NUEVO : ASUNTO_CONFIRMAR_EMAIL,
                "confirmar-email",
                Map.of(
                        "nombre",
                        confirmacion.nombre(),
                        "cambio",
                        confirmacion.cambio(),
                        "urlConfirmar",
                        urlConfirmar(confirmacion.token())));
    }

    private String urlConfirmar(String token) {
        return propiedades.baseUrl() + "/confirmar-email?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    /** El enlace lleva el token en claro; en la base de datos solo se guarda su hash. */
    public void enviarRecuperacion(String email, String nombre, String token) {
        String url = propiedades.baseUrl() + "/restablecer-password?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
        enviar(email, ASUNTO_RECUPERACION, "recuperar-password", Map.of("nombre", nombre, "urlRestablecer", url));
    }

    /** Confirmación de la baja (D-064). */
    public void enviarBaja(String email, String nombre) {
        enviar(email, ASUNTO_BAJA, "baja", Map.of("nombre", nombre));
    }

    /** Aviso de un cambio en el acceso a la cuenta (D-086). */
    public void enviarAvisoCuenta(AvisoCuenta aviso) {
        String asunto = switch (aviso.motivo()) {
            case EMAIL_CAMBIADO -> ASUNTO_EMAIL_CAMBIADO;
            case GOOGLE_VINCULADO -> ASUNTO_GOOGLE_VINCULADO;
            case GOOGLE_DESVINCULADO -> ASUNTO_GOOGLE_DESVINCULADO;
        };
        String mensaje = switch (aviso.motivo()) {
            case EMAIL_CAMBIADO ->
                "El email de tu cuenta de Toca ha cambiado a " + aviso.nuevoEmail()
                        + ". Desde ahora, los correos de la aplicación llegarán allí.";
            case GOOGLE_VINCULADO ->
                "Tu cuenta de Toca se ha vinculado con tu cuenta de Google: desde ahora "
                        + "también puedes entrar con ella.";
            case GOOGLE_DESVINCULADO ->
                "Tu cuenta de Toca ya no está vinculada con Google: para entrar, usa "
                        + "tu nombre de usuario o tu email y tu contraseña.";
        };
        enviar(
                aviso.email(),
                asunto,
                "aviso-cuenta",
                Map.of(
                        "nombre",
                        aviso.nombre(),
                        "titulo",
                        asunto,
                        "mensaje",
                        mensaje,
                        "urlLogin",
                        propiedades.baseUrl() + "/login"));
    }

    private void enviar(String para, String asunto, String plantilla, Map<String, Object> variables) {
        String html = plantillas.process(plantilla, new Context(ESPANOL, variables));
        MimeMessage mensaje = mailSender.createMimeMessage();
        try {
            MimeMessageHelper ayudante = new MimeMessageHelper(mensaje, false, StandardCharsets.UTF_8.name());
            ayudante.setFrom(propiedades.mailFrom(), REMITENTE);
            ayudante.setTo(para);
            ayudante.setSubject(asunto);
            ayudante.setText(html, true);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailPreparationException("No se pudo preparar el correo '" + asunto + "'", e);
        }
        mailSender.send(mensaje);
    }
}
