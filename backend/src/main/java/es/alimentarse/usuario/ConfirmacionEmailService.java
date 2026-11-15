package es.alimentarse.usuario;

import es.alimentarse.comun.TokenSeguro;
import es.alimentarse.comun.error.ExcepcionApi;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirmación del email con un enlace (D-088): al registrarse (va en el correo de bienvenida) y al cambiarlo. Un email
 * nuevo no sustituye al anterior hasta que se confirma. Sin confirmar se puede usar la aplicación, pero vincular Google
 * pide la contraseña (D-086).
 */
@Service
public class ConfirmacionEmailService {

    /** Lo que vale el enlace. */
    public static final Duration VALIDEZ = Duration.ofHours(24);

    static final String ENLACE_NO_VALE = "El enlace no es válido o ha caducado. Pide otro desde «Mi perfil».";
    static final String YA_CONFIRMADO = "Tu email ya está confirmado.";

    private final UsuarioMapper usuarios;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    public ConfirmacionEmailService(UsuarioMapper usuarios, ApplicationEventPublisher eventos, Clock reloj) {
        this.usuarios = usuarios;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    /**
     * Guarda un enlace nuevo para confirmar el email de la cuenta o, con {@code emailNuevo}, para cambiarlo por ese, y
     * devuelve su token. El enlace anterior, si lo había, deja de valer.
     */
    public String prepararEnlace(String id, @Nullable String emailNuevo) {
        String token = TokenSeguro.nuevo();
        usuarios.guardarTokenEmail(
                id, TokenSeguro.sha256(token), reloj.instant().plus(VALIDEZ), emailNuevo);
        return token;
    }

    /** Prepara el enlace y lo envía por correo al email que hay que confirmar. */
    public void enviarEnlace(String id, String nombre, String emailActual, @Nullable String emailNuevo) {
        String token = prepararEnlace(id, emailNuevo);
        eventos.publishEvent(new ConfirmacionEmailSolicitada(
                nombre, emailNuevo != null ? emailNuevo : emailActual, token, emailNuevo != null));
    }

    /**
     * Vuelve a enviar el enlace: al email nuevo si hay un cambio pendiente y, si no, al de la cuenta. 409 si no hay nada
     * que confirmar.
     */
    @Transactional
    public EnlaceEnviadoDto reenviar(String id) {
        Usuario usuario = buscar(id);
        if (usuario.emailPendiente() == null && usuario.emailVerificado()) {
            throw ExcepcionApi.conflicto("EMAIL_YA_CONFIRMADO", YA_CONFIRMADO, Map.of());
        }
        enviarEnlace(usuario.id(), usuario.nombre(), usuario.email(), usuario.emailPendiente());
        return new EnlaceEnviadoDto(usuario.emailPendiente() != null ? usuario.emailPendiente() : usuario.email());
    }

    /** Deja el email como está: el enlace enviado al nuevo deja de valer. */
    @Transactional
    public void cancelarCambio(String id) {
        buscar(id);
        usuarios.cancelarCambioEmail(id);
    }

    /**
     * Confirma el email con el token del enlace. No hace falta sesión: el enlace puede abrirse en otro dispositivo. Si
     * era un cambio, la cuenta pasa a usar el email nuevo, se avisa al anterior y el enlace de recuperación de la
     * contraseña que se hubiera pedido deja de valer. 400 si el enlace no vale; 409 si otra cuenta ya usa el email nuevo.
     */
    @Transactional
    public EmailConfirmadoDto confirmar(String token) {
        Usuario usuario = usuarios.buscarPorTokenEmail(TokenSeguro.sha256(token.strip()), reloj.instant())
                .orElseThrow(() -> ExcepcionApi.peticionIncorrecta("TOKEN_INVALIDO", ENLACE_NO_VALE, null));
        String nuevo = usuario.emailPendiente();
        if (nuevo != null && usuarios.existeEmail(nuevo, usuario.id())) {
            throw ExcepcionApi.conflicto(
                    "EMAIL_EN_USO", "Otra cuenta ya usa " + nuevo + ". Cambia el email desde «Mi perfil».", Map.of());
        }
        usuarios.confirmarEmail(usuario.id());
        if (nuevo != null) {
            usuarios.anularRecuperacion(usuario.id());
            // Las sesiones abiertas se cierran; la del navegador que confirma recibe un token nuevo (D-089)
            usuarios.anularSesiones(usuario.id());
            eventos.publishEvent(
                    new AvisoCuenta(usuario.nombre(), usuario.email(), AvisoCuenta.Motivo.EMAIL_CAMBIADO, nuevo));
        }
        return new EmailConfirmadoDto(usuario.id(), nuevo != null ? nuevo : usuario.email());
    }

    private Usuario buscar(String id) {
        return usuarios.buscarPorId(id).orElseThrow(() -> ExcepcionApi.noEncontrado("El usuario no existe."));
    }
}
