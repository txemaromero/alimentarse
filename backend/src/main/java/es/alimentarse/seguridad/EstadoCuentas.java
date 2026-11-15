package es.alimentarse.seguridad;

import es.alimentarse.usuario.UsuarioMapper;
import org.springframework.stereotype.Component;

/**
 * Comprueba en cada petición con token que la cuenta sigue existiendo, no está bloqueada (D-063) y el token sigue siendo
 * de su versión de sesión actual (D-070). Un token válido dura 8 horas (D-019): sin esta comprobación, un usuario
 * bloqueado o dado de baja, o quien tuviera un token anterior a un cambio de contraseña o de rol, seguiría usando la
 * API hasta que caducara.
 */
@Component
class EstadoCuentas {

    enum Estado {
        ACTIVA,
        BLOQUEADA,
        INEXISTENTE,
        ANULADA
    }

    private final UsuarioMapper usuarios;

    EstadoCuentas(UsuarioMapper usuarios) {
        this.usuarios = usuarios;
    }

    Estado de(String id, int versionDelToken) {
        return usuarios.estadoSesion(id)
                .map(cuenta -> {
                    if (cuenta.bloqueado()) {
                        return Estado.BLOQUEADA;
                    }
                    return cuenta.version() == versionDelToken ? Estado.ACTIVA : Estado.ANULADA;
                })
                .orElse(Estado.INEXISTENTE);
    }
}
