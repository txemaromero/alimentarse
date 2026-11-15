package es.alimentarse.admin;

import es.alimentarse.ingrediente.IngredienteDto;
import es.alimentarse.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
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

/** Grupos de alimentos e ingredientes en la administración (D-065). {@code /api/admin/**} exige el rol ADMIN. */
@RestController
@RequestMapping("/api/admin")
public class CatalogoAdminController {

    private final CatalogoAdminService catalogo;

    public CatalogoAdminController(CatalogoAdminService catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping("/grupos")
    public List<GrupoAdminDto> grupos(@AuthenticationPrincipal UsuarioAutenticado yo) {
        return catalogo.grupos(yo.id());
    }

    @PostMapping("/grupos")
    public ResponseEntity<GrupoAdminDto> crearGrupo(
            @Valid @RequestBody GrupoRequest datos, @AuthenticationPrincipal UsuarioAutenticado yo) {
        GrupoAdminDto creado = catalogo.crearGrupo(datos, yo.id());
        return ResponseEntity.created(URI.create("/api/admin/grupos/" + creado.id()))
                .body(creado);
    }

    @PutMapping("/grupos/{id}")
    public GrupoAdminDto editarGrupo(
            @PathVariable int id,
            @Valid @RequestBody GrupoRequest datos,
            @AuthenticationPrincipal UsuarioAutenticado yo) {
        return catalogo.editarGrupo(id, datos, yo.id());
    }

    @DeleteMapping("/grupos/{id}")
    public ResponseEntity<Void> borrarGrupo(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado yo) {
        catalogo.borrarGrupo(id, yo.id());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/ingredientes/{id}/fusionar")
    public FusionDto fusionar(
            @PathVariable int id,
            @Valid @RequestBody FusionRequest datos,
            @AuthenticationPrincipal UsuarioAutenticado yo) {
        return catalogo.fusionarIngredientes(id, datos.destino(), yo.id());
    }

    @DeleteMapping("/ingredientes/{id}")
    public ResponseEntity<Void> borrarIngrediente(
            @PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado yo) {
        catalogo.borrarIngrediente(id, yo.id());
        return ResponseEntity.noContent().build();
    }

    /** Alimentos de BEDCA para asignar a un ingrediente (D-080), hasta 10. */
    @GetMapping("/alimentos-bedca")
    public List<AlimentoBedcaDto> alimentos(
            @RequestParam(defaultValue = "") @Size(max = 100, message = "Busca con 100 caracteres como mucho") String q,
            @AuthenticationPrincipal UsuarioAutenticado yo) {
        return catalogo.buscarAlimentos(q, yo.id());
    }

    /** El alimento de BEDCA y los pesos de las piezas del ingrediente, para editarlos (D-080, D-081). */
    @GetMapping("/ingredientes/{id}/nutricion")
    public NutricionIngredienteDto nutricion(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado yo) {
        return catalogo.nutricion(id, yo.id());
    }

    /** Sustituye el alimento de BEDCA y los pesos de las piezas del ingrediente (D-080, D-081). */
    @PutMapping("/ingredientes/{id}/nutricion")
    public IngredienteDto guardarNutricion(
            @PathVariable int id,
            @Valid @RequestBody NutricionIngredienteRequest datos,
            @AuthenticationPrincipal UsuarioAutenticado yo) {
        return catalogo.guardarNutricion(id, datos, yo.id());
    }
}
