package es.alimentarse.auth;

import es.alimentarse.comun.DatosEnLog;
import es.alimentarse.config.AppProperties;
import es.alimentarse.seguridad.CifradoPassword;
import es.alimentarse.seguridad.UsuarioAutenticado;
import es.alimentarse.usuario.Usuario;
import es.alimentarse.usuario.UsuarioMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea el usuario {@code admin} (roles USER y ADMIN) en el primer arranque, con la contraseña de
 * ADMIN_INITIAL_PASSWORD (sección 4). Si ya existe, no lo toca. Tampoco lo crea si ya hay algún administrador: si
 * {@code admin} se dio de baja, no reaparece al reiniciar (D-064).
 */
@Component
class AdminInicial implements ApplicationRunner {

    static final String ID = "admin";
    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);
    private static final int LONGITUD_MINIMA_PASSWORD = 8;

    private final UsuarioMapper usuarios;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties.AdminInicial configuracion;

    AdminInicial(UsuarioMapper usuarios, PasswordEncoder passwordEncoder, AppProperties propiedades) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.configuracion = propiedades.adminInicial();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (usuarios.existeId(ID) || usuarios.hayAdministradores()) {
            return;
        }
        String password = configuracion.password();
        if (password == null || password.isBlank()) {
            log.warn("No se ha creado el usuario administrador inicial: define ADMIN_INITIAL_PASSWORD.");
            return;
        }
        if (password.length() < LONGITUD_MINIMA_PASSWORD) {
            log.warn(
                    "No se ha creado el usuario administrador inicial: ADMIN_INITIAL_PASSWORD debe tener al menos {}"
                            + " caracteres.",
                    LONGITUD_MINIMA_PASSWORD);
            return;
        }
        if (!CifradoPassword.cabe(password)) {
            // bcrypt no la cifraría y el arranque fallaría (D-090)
            log.warn(
                    "No se ha creado el usuario administrador inicial: ADMIN_INITIAL_PASSWORD no puede ocupar más de {}"
                            + " bytes (la ñ y las tildes ocupan 2).",
                    CifradoPassword.MAXIMO_BYTES);
            return;
        }
        if (usuarios.existeEmail(configuracion.email(), null)) {
            log.warn(
                    "No se ha creado el usuario administrador inicial: el email {} ya lo usa otra cuenta."
                            + " Cambia ADMIN_INITIAL_EMAIL.",
                    DatosEnLog.email(configuracion.email()));
            return;
        }
        usuarios.insertar(Usuario.nuevo(
                ID, "Administrador", "Toca", null, configuracion.email(), passwordEncoder.encode(password)));
        usuarios.asignarRol(ID, UsuarioAutenticado.ROL_USER);
        usuarios.asignarRol(ID, UsuarioAutenticado.ROL_ADMIN);
        log.info("Creado el usuario administrador inicial '{}' ({}).", ID, DatosEnLog.email(configuracion.email()));
    }
}
