package es.alimentarse.admin;

import es.alimentarse.comun.Pagina;
import es.alimentarse.comun.Texto;
import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.receta.RecetaDetalleDto;
import es.alimentarse.receta.RecetaResumenDto;
import es.alimentarse.receta.RecetaService;
import es.alimentarse.receta.ValoracionDto;
import es.alimentarse.receta.ValoracionService;
import es.alimentarse.seguridad.UsuarioAutenticado;
import es.alimentarse.usuario.UsuarioMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administración (7): gestión de usuarios y moderación (D-063). El rol del solicitante se comprueba en la base de
 * datos: un administrador degradado con un token antiguo, que aún dice ADMIN, ya no puede administrar (D-043).
 */
@Service
public class AdminService {

    static final String PROPIO_ROL = "No puedes quitarte el rol de administrador a ti mismo.";
    static final String ULTIMO_ADMINISTRADOR = "Debe quedar al menos un administrador.";
    static final String PROPIO_BLOQUEO = "No puedes bloquearte a ti mismo.";
    static final String ADMIN_NO_BLOQUEABLE =
            "No se puede bloquear a un administrador: quítale antes el rol de administrador.";
    static final String USUARIO_BLOQUEADO = "Ese usuario está bloqueado: desbloquéalo antes de hacerlo administrador.";

    private final AdminMapper admin;
    private final UsuarioMapper usuarios;
    private final RecetaService recetas;
    private final ValoracionService valoraciones;

    public AdminService(
            AdminMapper admin, UsuarioMapper usuarios, RecetaService recetas, ValoracionService valoraciones) {
        this.admin = admin;
        this.usuarios = usuarios;
        this.recetas = recetas;
        this.valoraciones = valoraciones;
    }

    /** Usuarios ordenados por id; {@code q} busca en id, nombre, apellidos o email (D-044). */
    @Transactional(readOnly = true)
    public Pagina<UsuarioAdminDto> listar(String q, String yo, int pagina, int tamano) {
        exigirAdministrador(yo);
        String patron = Texto.patronContiene(q);
        long total = admin.contar(patron);
        long desplazamiento = Pagina.desplazamiento(pagina, tamano);
        List<UsuarioAdminDto> contenido =
                desplazamiento >= total ? List.of() : admin.buscar(patron, desplazamiento, tamano);
        return Pagina.de(contenido, pagina, tamano, total);
    }

    /**
     * Da o quita el rol ADMIN; idempotente. Los cambios se serializan con un bloqueo, así que dos administradores que
     * se degradan mutuamente a la vez no pueden dejar el sistema sin ninguno.
     */
    @Transactional
    public UsuarioAdminDto cambiarAdmin(String id, boolean hacerAdmin, String yo) {
        admin.bloquearRolAdmin();
        exigirAdministrador(yo);
        UsuarioAdminDto usuario = buscar(id);
        if (usuario.admin() == hacerAdmin) {
            return usuario;
        }
        if (hacerAdmin) {
            if (usuario.bloqueado()) {
                throw ExcepcionApi.conflicto("USUARIO_BLOQUEADO", USUARIO_BLOQUEADO, null);
            }
            usuarios.asignarRol(usuario.id(), UsuarioAutenticado.ROL_ADMIN);
        } else {
            if (usuario.id().equalsIgnoreCase(yo)) {
                throw ExcepcionApi.peticionIncorrecta("PROPIO_ROL", PROPIO_ROL, null);
            }
            if (admin.contarAdmins() <= 1) {
                throw ExcepcionApi.conflicto("ULTIMO_ADMINISTRADOR", ULTIMO_ADMINISTRADOR, null);
            }
            usuarios.quitarRol(usuario.id(), UsuarioAutenticado.ROL_ADMIN);
        }
        // Tiene que volver a entrar: su token aún lleva los roles anteriores (D-070)
        usuarios.anularSesiones(usuario.id());
        return buscar(id);
    }

    /**
     * Bloquea o desbloquea a un usuario; idempotente (D-063). Un bloqueado no puede iniciar sesión y su sesión abierta
     * deja de valer en la siguiente petición. A un administrador no se le puede bloquear: primero hay que quitarle el
     * rol. Comparte el bloqueo de {@link #cambiarAdmin}, para que no se cruce con un cambio de rol.
     */
    @Transactional
    public UsuarioAdminDto cambiarBloqueo(String id, boolean bloquear, String yo) {
        admin.bloquearRolAdmin();
        exigirAdministrador(yo);
        UsuarioAdminDto usuario = buscar(id);
        if (usuario.bloqueado() == bloquear) {
            return usuario;
        }
        if (bloquear) {
            if (usuario.id().equalsIgnoreCase(yo)) {
                throw ExcepcionApi.peticionIncorrecta("PROPIO_BLOQUEO", PROPIO_BLOQUEO, null);
            }
            if (usuario.admin()) {
                throw ExcepcionApi.conflicto("ADMIN_NO_BLOQUEABLE", ADMIN_NO_BLOQUEABLE, null);
            }
        }
        usuarios.actualizarBloqueado(usuario.id(), bloquear);
        if (bloquear) {
            // Los tokens anteriores al bloqueo no vuelven a valer cuando se desbloquee (D-089)
            usuarios.anularSesiones(usuario.id());
        }
        return buscar(id);
    }

    /** Recetas ocultas por la administración, para revisarlas y volver a mostrarlas (D-063). */
    @Transactional(readOnly = true)
    public Pagina<RecetaResumenDto> recetasOcultas(String yo, int pagina, int tamano) {
        exigirAdministrador(yo);
        return recetas.ocultas(yo, pagina, tamano);
    }

    /** Oculta una receta con un motivo, que verá su autor, o la vuelve a mostrar (D-063). */
    @Transactional
    public RecetaDetalleDto ocultarReceta(int id, boolean oculta, String motivo, String yo) {
        exigirAdministrador(yo);
        return recetas.ocultar(id, oculta, motivo, yo);
    }

    /** Oculta una valoración, que deja de verse y de contar en la media, o la vuelve a mostrar (D-077). */
    @Transactional
    public ValoracionDto ocultarValoracion(long id, boolean oculta, String yo) {
        exigirAdministrador(yo);
        return valoraciones.ocultar(id, oculta);
    }

    private UsuarioAdminDto buscar(String id) {
        return admin.buscarPorId(id).orElseThrow(() -> ExcepcionApi.noEncontrado("El usuario no existe."));
    }

    /** También lo usa {@link CatalogoAdminService}. */
    void exigirAdministrador(String yo) {
        if (!admin.esAdmin(yo)) {
            throw ExcepcionApi.prohibido("Ya no tienes permisos de administrador. Vuelve a iniciar sesión.");
        }
    }
}
