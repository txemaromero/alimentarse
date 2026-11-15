package es.alimentarse.alimentacion;

import es.alimentarse.consumo.ConsumoDto;
import es.alimentarse.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Planificador semanal del usuario actual (D-068). */
@RestController
@RequestMapping("/api/plan")
public class PlanController {

    private final PlanService plan;

    public PlanController(PlanService plan) {
        this.plan = plan;
    }

    @GetMapping
    public PlanDto plan(
            @RequestParam Instant desde, @RequestParam Instant hasta, @AuthenticationPrincipal UsuarioAutenticado yo) {
        return plan.plan(desde, hasta, yo.id());
    }

    /** Lista de la compra de las comidas del intervalo que aún no han pasado (D-076). */
    @GetMapping("/compra")
    public ListaCompraDto compra(
            @RequestParam Instant desde, @RequestParam Instant hasta, @AuthenticationPrincipal UsuarioAutenticado yo) {
        return plan.listaCompra(desde, hasta, yo.id());
    }

    @PostMapping
    public ResponseEntity<ComidaPlanificadaDto> anadir(
            @Valid @RequestBody ComidaPlanRequest datos, @AuthenticationPrincipal UsuarioAutenticado yo) {
        ComidaPlanificadaDto comida = plan.anadir(datos, yo.id());
        return ResponseEntity.created(URI.create("/api/plan/" + comida.id())).body(comida);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrar(@PathVariable long id, @AuthenticationPrincipal UsuarioAutenticado yo) {
        plan.borrar(id, yo.id());
        return ResponseEntity.noContent().build();
    }

    /** La comida pasa a los consumos y sale del plan. */
    @PostMapping("/{id}/consumir")
    public ResponseEntity<ConsumoDto> consumir(@PathVariable long id, @AuthenticationPrincipal UsuarioAutenticado yo) {
        ConsumoDto consumo = plan.consumir(id, yo.id());
        return ResponseEntity.created(URI.create("/api/consumos/" + consumo.id()))
                .body(consumo);
    }
}
