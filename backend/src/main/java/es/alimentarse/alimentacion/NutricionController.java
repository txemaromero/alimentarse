package es.alimentarse.alimentacion;

import es.alimentarse.seguridad.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Estudio nutricional de las recetas (D-079) y de lo comido en la semana (D-082), con los datos de BEDCA. */
@RestController
public class NutricionController {

    private final NutricionService nutricion;

    public NutricionController(NutricionService nutricion) {
        this.nutricion = nutricion;
    }

    @GetMapping("/api/recetas/{id}/nutricion")
    public NutricionRecetaDto deReceta(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado yo) {
        return nutricion.deReceta(id, yo.id(), yo.esAdmin());
    }

    /** Nutrientes de lo comido en los últimos 7 días (D-082). */
    @GetMapping("/api/alimentacion/nutricion")
    public NutricionSemanaDto semana(@AuthenticationPrincipal UsuarioAutenticado yo) {
        return nutricion.semana(yo.id());
    }
}
