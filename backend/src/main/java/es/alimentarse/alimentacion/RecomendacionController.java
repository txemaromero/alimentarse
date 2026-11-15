package es.alimentarse.alimentacion;

import es.alimentarse.comun.Pagina;
import es.alimentarse.receta.FiltroRecetas;
import es.alimentarse.receta.ParametrosFiltroRecetas;
import es.alimentarse.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * «¿Qué toca hoy?» (6.3). Está en este paquete, y no en el de recetas, para que {@code receta} no dependa de
 * {@code alimentacion}; la ruta literal tiene prioridad sobre {@code /api/recetas/{id}}.
 */
@RestController
public class RecomendacionController {

    private final RecomendacionService recomendacion;

    public RecomendacionController(RecomendacionService recomendacion) {
        this.recomendacion = recomendacion;
    }

    @GetMapping("/api/recetas/recomendadas")
    public Pagina<RecetaRecomendadaDto> recomendadas(
            @Valid @ModelAttribute @ParameterObject ParametrosFiltroRecetas filtro,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @RequestParam(defaultValue = "9")
                    @Min(value = 1, message = "El tamaño de página debe estar entre 1 y 100")
                    @Max(value = 100, message = "El tamaño de página debe estar entre 1 y 100")
                    int size,
            @AuthenticationPrincipal UsuarioAutenticado yo) {
        return recomendacion.recomendadas(FiltroRecetas.de(filtro), yo.id(), page, size);
    }

    /** Aviso previo a consumir: cómo quedarían los grupos con límite de la receta (D-067). */
    @GetMapping("/api/recetas/{id}/impacto")
    public List<ImpactoGrupoDto> impacto(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado yo) {
        return recomendacion.impacto(id, yo.id());
    }
}
