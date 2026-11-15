package es.alimentarse.ingrediente;

import es.alimentarse.comun.Pagina;
import es.alimentarse.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingredientes")
public class IngredienteController {

    private final IngredienteService ingredientes;

    public IngredienteController(IngredienteService ingredientes) {
        this.ingredientes = ingredientes;
    }

    /** Listado paginado (16 por página por defecto), ordenado por nombre. */
    @GetMapping
    public Pagina<IngredienteDto> listar(
            @RequestParam(required = false) @Size(max = 100, message = "Busca con 100 caracteres como mucho")
                    String nombre,
            @RequestParam(required = false) Integer grupo,
            @RequestParam(defaultValue = "false") boolean sinAlimento,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @RequestParam(defaultValue = "16")
                    @Min(value = 1, message = "El tamaño de página debe estar entre 1 y 100")
                    @Max(value = 100, message = "El tamaño de página debe estar entre 1 y 100")
                    int size) {
        return ingredientes.listar(nombre, grupo, sinAlimento, page, size);
    }

    @GetMapping("/sugerencias")
    public List<IngredienteDto> sugerencias(
            @RequestParam(defaultValue = "") @Size(max = 100, message = "Busca con 100 caracteres como mucho")
                    String q) {
        return ingredientes.sugerencias(q);
    }

    /** Cualquier usuario con el email confirmado puede crear ingredientes, con un tope diario (D-089). */
    @PostMapping
    public ResponseEntity<IngredienteDto> crear(
            @AuthenticationPrincipal UsuarioAutenticado yo, @Valid @RequestBody IngredienteRequest datos) {
        IngredienteDto creado = ingredientes.crear(datos, yo);
        return ResponseEntity.created(URI.create("/api/ingredientes/" + creado.id()))
                .body(creado);
    }

    /** Solo un administrador puede editarlos (6.8). */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public IngredienteDto editar(@PathVariable int id, @Valid @RequestBody IngredienteRequest datos) {
        return ingredientes.editar(id, datos);
    }
}
