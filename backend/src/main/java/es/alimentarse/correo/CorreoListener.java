package es.alimentarse.correo;

import es.alimentarse.auth.RecuperacionPasswordSolicitada;
import es.alimentarse.auth.UsuarioRegistrado;
import es.alimentarse.comun.DatosEnLog;
import es.alimentarse.cuenta.CuentaEliminada;
import es.alimentarse.usuario.AvisoCuenta;
import es.alimentarse.usuario.ConfirmacionEmailSolicitada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envía los correos en segundo plano y solo si la transacción que los provoca se ha confirmado. Un fallo del servidor
 * de correo no deshace el registro: queda en el log (D-020).
 */
@Component
class CorreoListener {

    private static final Logger log = LoggerFactory.getLogger(CorreoListener.class);

    private final CorreoService correo;

    CorreoListener(CorreoService correo) {
        this.correo = correo;
    }

    @Async
    @TransactionalEventListener
    void alRegistrarse(UsuarioRegistrado evento) {
        try {
            correo.enviarBienvenida(evento.email(), evento.nombre(), evento.tokenConfirmacion());
        } catch (RuntimeException e) {
            registrarFallo("el correo de bienvenida", evento.email(), e);
        }
    }

    @Async
    @TransactionalEventListener
    void alDarseDeBaja(CuentaEliminada evento) {
        try {
            correo.enviarBaja(evento.email(), evento.nombre());
        } catch (RuntimeException e) {
            registrarFallo("la confirmación de la baja", evento.email(), e);
        }
    }

    @Async
    @TransactionalEventListener
    void alPedirLaConfirmacion(ConfirmacionEmailSolicitada evento) {
        try {
            correo.enviarConfirmacionEmail(evento);
        } catch (RuntimeException e) {
            registrarFallo("el enlace de confirmación", evento.email(), e);
        }
    }

    @Async
    @TransactionalEventListener
    void alCambiarElAcceso(AvisoCuenta evento) {
        try {
            correo.enviarAvisoCuenta(evento);
        } catch (RuntimeException e) {
            registrarFallo("el aviso " + evento.motivo(), evento.email(), e);
        }
    }

    @Async
    @TransactionalEventListener
    void alSolicitarRecuperacion(RecuperacionPasswordSolicitada evento) {
        try {
            correo.enviarRecuperacion(evento.email(), evento.nombre(), evento.token());
        } catch (RuntimeException e) {
            registrarFallo("el correo de recuperación", evento.email(), e);
        }
    }

    /**
     * Sin el email completo (D-089): la dirección enmascarada y el error con las direcciones también enmascaradas. La
     * traza, que puede repetirla, solo con el nivel DEBUG.
     */
    private static void registrarFallo(String que, String email, RuntimeException e) {
        log.error("No se pudo enviar {} a {}: {}", que, DatosEnLog.email(email), DatosEnLog.sinEmails(e.toString()));
        log.debug("Traza del fallo al enviar {}", que, e);
    }
}
