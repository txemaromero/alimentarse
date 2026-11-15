package es.alimentarse.consumo;

import es.alimentarse.comun.Pagina;
import es.alimentarse.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consumos del usuario actual (6.7). */
@RestController
@RequestMapping("/api/consumos")
public class ConsumoController {

    private final ConsumoService consumos;

    public ConsumoController(ConsumoService consumos) {
        this.consumos = consumos;
    }

    @PostMapping
    public ResponseEntity<ConsumoDto> registrar(
            @Valid @RequestBody ConsumoRequest datos, @AuthenticationPrincipal UsuarioAutenticado yo) {
        ConsumoDto consumo = consumos.registrar(datos, yo.id());
        return ResponseEntity.created(URI.create("/api/consumos/" + consumo.id()))
                .body(consumo);
    }

    /** Histórico, del más reciente al más antiguo. */
    @GetMapping
    public Pagina<ConsumoDto> historial(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @RequestParam(defaultValue = "9")
                    @Min(value = 1, message = "El tamaño de página debe estar entre 1 y 100")
                    @Max(value = 100, message = "El tamaño de página debe estar entre 1 y 100")
                    int size,
            @AuthenticationPrincipal UsuarioAutenticado yo) {
        return consumos.historial(yo.id(), page, size);
    }

    @PutMapping("/{id}")
    public ConsumoDto cambiarFecha(
            @PathVariable long id,
            @Valid @RequestBody FechaConsumoRequest datos,
            @AuthenticationPrincipal UsuarioAutenticado yo) {
        return consumos.cambiarFecha(id, datos, yo.id());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrar(@PathVariable long id, @AuthenticationPrincipal UsuarioAutenticado yo) {
        consumos.borrar(id, yo.id());
        return ResponseEntity.noContent().build();
    }
}
