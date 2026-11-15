package es.alimentarse.usuario;

import es.alimentarse.comun.Texto;
import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.seguridad.JwtService;
import es.alimentarse.seguridad.UsuarioAutenticado;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Perfil del propio usuario: consulta, edición y cambio de contraseña. */
@Service
public class UsuarioService {

    static final String EMAIL_EN_USO = "Ya hay otra cuenta con ese email";
    static final String PASSWORD_ACTUAL_INCORRECTA = "La contraseña actual no es correcta";
    static final String PASSWORD_PARA_EMAIL = "Escribe tu contraseña actual para cambiar el email";

    private final UsuarioMapper usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;
    private final ApplicationEventPublisher eventos;
    private final ConfirmacionEmailService confirmaciones;

    public UsuarioService(
            UsuarioMapper usuarios,
            PasswordEncoder passwordEncoder,
            JwtService jwt,
            ApplicationEventPublisher eventos,
            ConfirmacionEmailService confirmaciones) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
        this.eventos = eventos;
        this.confirmaciones = confirmaciones;
    }

    @Transactional(readOnly = true)
    public PerfilDto perfil(String id) {
        return PerfilDto.de(buscar(id), usuarios.rolesDe(id));
    }

    /**
     * Cambia los datos personales. Cambiar el email pide la contraseña actual (D-086): con solo una sesión abierta no se
     * puede quitar la cuenta a su dueño. El email nuevo no se usa hasta que se confirma con el enlace que se le envía
     * (D-088); al confirmarlo se avisa al anterior ({@link ConfirmacionEmailService#confirmar}).
     */
    @Transactional
    public PerfilDto actualizarPerfil(String id, ActualizarPerfilRequest datos) {
        Usuario usuario = buscar(id);
        String email = datos.email().strip();
        boolean cambiaEmail = !usuario.email().equalsIgnoreCase(email);
        if (cambiaEmail) {
            exigirPassword(usuario, datos.password(), PASSWORD_PARA_EMAIL);
        }
        if (usuarios.existeEmail(email, id)) {
            throw ExcepcionApi.conflicto(EMAIL_EN_USO + ".", Map.of("email", EMAIL_EN_USO));
        }
        // Otras mayúsculas son el mismo email y se guardan ya; uno nuevo, cuando se confirme
        usuarios.actualizarPerfil(
                id,
                datos.nombre().strip(),
                datos.apellido1().strip(),
                Texto.opcional(datos.apellido2()),
                cambiaEmail ? usuario.email() : email);
        if (cambiaEmail) {
            confirmaciones.enviarEnlace(id, datos.nombre().strip(), usuario.email(), email);
        }
        return perfil(id);
    }

    /** Deja de poder entrar con Google (D-086). Pide la contraseña: sin ella, después no podría entrar. */
    @Transactional
    public PerfilDto desvincularGoogle(String id, DesvincularGoogleRequest datos) {
        Usuario usuario = buscar(id);
        exigirPassword(usuario, datos.password(), PASSWORD_ACTUAL_INCORRECTA);
        if (usuario.googleId() != null) {
            usuarios.desvincularGoogle(id);
            eventos.publishEvent(
                    new AvisoCuenta(usuario.nombre(), usuario.email(), AvisoCuenta.Motivo.GOOGLE_DESVINCULADO, null));
        }
        return perfil(id);
    }

    /** 400 con el error en el campo {@code password} si falta o no es la de la cuenta. */
    private void exigirPassword(Usuario usuario, @Nullable String password, String siFalta) {
        if (password == null || password.isEmpty()) {
            throw ExcepcionApi.peticionIncorrecta("PASSWORD_INCORRECTA", siFalta + ".", Map.of("password", siFalta));
        }
        if (!passwordEncoder.matches(password, usuario.password())) {
            throw ExcepcionApi.peticionIncorrecta(
                    "PASSWORD_INCORRECTA",
                    PASSWORD_ACTUAL_INCORRECTA + ".",
                    Map.of("password", PASSWORD_ACTUAL_INCORRECTA));
        }
    }

    /**
     * Cambia la contraseña y anula las sesiones abiertas (D-070). Devuelve un token nuevo para seguir en este dispositivo.
     */
    @Transactional
    public String cambiarPassword(String id, CambioPasswordRequest datos) {
        Usuario usuario = buscar(id);
        if (!passwordEncoder.matches(datos.actual(), usuario.password())) {
            throw ExcepcionApi.peticionIncorrecta(
                    "PASSWORD_INCORRECTA",
                    PASSWORD_ACTUAL_INCORRECTA + ".",
                    Map.of("actual", PASSWORD_ACTUAL_INCORRECTA));
        }
        usuarios.actualizarPassword(id, passwordEncoder.encode(datos.nueva()));
        return jwt.emitir(
                        new UsuarioAutenticado(id, usuario.nombre(), usuarios.rolesDe(id)), usuarios.versionSesion(id))
                .valor();
    }

    private Usuario buscar(String id) {
        return usuarios.buscarPorId(id).orElseThrow(() -> ExcepcionApi.noEncontrado("El usuario no existe."));
    }
}
