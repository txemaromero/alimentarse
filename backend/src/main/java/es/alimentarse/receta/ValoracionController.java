package es.alimentarse.receta;

import es.alimentarse.comun.Pagina;
import es.alimentarse.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Valoraciones de una receta (D-077). */
@RestController
@RequestMapping("/api/recetas/{id}")
public class ValoracionController {

    private final ValoracionService valoraciones;

    public ValoracionController(ValoracionService valoraciones) {
        this.valoraciones = valoraciones;
    }

    @GetMapping("/valoraciones")
    public Pagina<ValoracionDto> listar(
            @PathVariable int id,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @RequestParam(defaultValue = "5")
                    @Min(value = 1, message = "El tamaño de página debe estar entre 1 y 100")
                    @Max(value = 100, message = "El tamaño de página debe estar entre 1 y 100")
                    int size,
            @AuthenticationPrincipal UsuarioAutenticado yo) {
        return valoraciones.listar(id, yo.id(), yo.esAdmin(), page, size);
    }

    /** Crea o cambia la valoración propia. */
    @PutMapping("/valoracion")
    public ResumenValoracionesDto valorar(
            @PathVariable int id,
            @Valid @RequestBody ValoracionRequest datos,
            @AuthenticationPrincipal UsuarioAutenticado yo) {
        return valoraciones.valorar(id, datos, yo.id());
    }

    @DeleteMapping("/valoracion")
    public ResumenValoracionesDto borrar(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado yo) {
        return valoraciones.borrar(id, yo.id());
    }
}
