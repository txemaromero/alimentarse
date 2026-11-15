package es.alimentarse.admin;

import es.alimentarse.comun.Pagina;
import es.alimentarse.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Gestión de usuarios. {@code /api/admin/**} exige el rol ADMIN (SecurityConfig). */
@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminController {

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping
    public Pagina<UsuarioAdminDto> listar(
            @RequestParam(defaultValue = "") @Size(max = 100, message = "Busca con 100 caracteres como mucho") String q,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @RequestParam(defaultValue = "10")
                    @Min(value = 1, message = "El tamaño de página debe estar entre 1 y 100")
                    @Max(value = 100, message = "El tamaño de página debe estar entre 1 y 100")
                    int size,
            @AuthenticationPrincipal UsuarioAutenticado yo) {
        return admin.listar(q, yo.id(), page, size);
    }

    /** Un usuario bloqueado no puede iniciar sesión, y su sesión abierta deja de valer al momento (D-063). */
    @PutMapping("/{id}/bloqueado")
    public UsuarioAdminDto cambiarBloqueo(
            @PathVariable String id,
            @Valid @RequestBody CambioBloqueoRequest datos,
            @AuthenticationPrincipal UsuarioAutenticado yo) {
        return admin.cambiarBloqueo(id, datos.bloqueado(), yo.id());
    }

    /** El cambio se aplica al usuario afectado en su siguiente inicio de sesión (D-019). */
    @PutMapping("/{id}/admin")
    public UsuarioAdminDto cambiarAdmin(
            @PathVariable String id,
            @Valid @RequestBody CambioAdminRequest datos,
            @AuthenticationPrincipal UsuarioAutenticado yo) {
        return admin.cambiarAdmin(id, datos.admin(), yo.id());
    }
}
