package es.alimentarse.admin;

import es.alimentarse.receta.ValoracionDto;
import es.alimentarse.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Moderación de las valoraciones (D-077). {@code /api/admin/**} exige el rol ADMIN, y el servicio lo comprueba en la
 * BD (D-043).
 */
@RestController
@RequestMapping("/api/admin/valoraciones")
public class ModeracionValoracionController {

    private final AdminService admin;

    public ModeracionValoracionController(AdminService admin) {
        this.admin = admin;
    }

    @PutMapping("/{id}/oculta")
    public ValoracionDto ocultar(
            @PathVariable long id,
            @Valid @RequestBody OcultarValoracionRequest datos,
            @AuthenticationPrincipal UsuarioAutenticado yo) {
        return admin.ocultarValoracion(id, datos.oculta(), yo.id());
    }
}
