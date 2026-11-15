package es.alimentarse.admin;

import es.alimentarse.comun.Pagina;
import es.alimentarse.receta.RecetaDetalleDto;
import es.alimentarse.receta.RecetaResumenDto;
import es.alimentarse.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Moderación de recetas (D-063). {@code /api/admin/**} exige el rol ADMIN, y el servicio lo comprueba en la BD. */
@RestController
@RequestMapping("/api/admin/recetas")
public class ModeracionController {

    private final AdminService admin;

    public ModeracionController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/ocultas")
    public Pagina<RecetaResumenDto> ocultas(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @RequestParam(defaultValue = "9")
                    @Min(value = 1, message = "El tamaño de página debe estar entre 1 y 100")
                    @Max(value = 100, message = "El tamaño de página debe estar entre 1 y 100")
                    int size,
            @AuthenticationPrincipal UsuarioAutenticado yo) {
        return admin.recetasOcultas(yo.id(), page, size);
    }

    @PutMapping("/{id}/oculta")
    public RecetaDetalleDto ocultar(
            @PathVariable int id,
            @Valid @RequestBody ModeracionRecetaRequest datos,
            @AuthenticationPrincipal UsuarioAutenticado yo) {
        return admin.ocultarReceta(id, datos.oculta(), datos.motivo(), yo.id());
    }
}
