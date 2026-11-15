package es.alimentarse.receta;

import es.alimentarse.comun.Pagina;
import es.alimentarse.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Recetas. El alta y la edición son multipart: parte {@code receta} (JSON) y parte {@code imagen} opcional (D-036).
 */
@RestController
@RequestMapping("/api/recetas")
public class RecetaController {

    private final RecetaService recetas;

    public RecetaController(RecetaService recetas) {
        this.recetas = recetas;
    }

    /** Recetas publicadas con los filtros de la sección 6.5, 9 por página. */
    @GetMapping
    public Pagina<RecetaResumenDto> listar(
            @Valid @ModelAttribute @ParameterObject ParametrosFiltroRecetas filtro,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @RequestParam(defaultValue = "9")
                    @Min(value = 1, message = "El tamaño de página debe estar entre 1 y 100")
                    @Max(value = 100, message = "El tamaño de página debe estar entre 1 y 100")
                    int size,
            @AuthenticationPrincipal UsuarioAutenticado yo) {
        return recetas.listar(FiltroRecetas.de(filtro), yo.id(), page, size);
    }

    /** Recetas propias, publicadas o no, con los mismos filtros. */
    @GetMapping("/mias")
    public Pagina<RecetaResumenDto> mias(
            @Valid @ModelAttribute @ParameterObject ParametrosFiltroRecetas filtro,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @RequestParam(defaultValue = "9")
                    @Min(value = 1, message = "El tamaño de página debe estar entre 1 y 100")
                    @Max(value = 100, message = "El tamaño de página debe estar entre 1 y 100")
                    int size,
            @AuthenticationPrincipal UsuarioAutenticado yo) {
        return recetas.mias(FiltroRecetas.de(filtro), yo.id(), page, size);
    }

    /** Recetas publicadas a las que el usuario ha dado «me gusta», con los mismos filtros (D-062). */
    @GetMapping("/me-gustan")
    public Pagina<RecetaResumenDto> meGustan(
            @Valid @ModelAttribute @ParameterObject ParametrosFiltroRecetas filtro,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @RequestParam(defaultValue = "9")
                    @Min(value = 1, message = "El tamaño de página debe estar entre 1 y 100")
                    @Max(value = 100, message = "El tamaño de página debe estar entre 1 y 100")
                    int size,
            @AuthenticationPrincipal UsuarioAutenticado yo) {
        return recetas.meGustan(FiltroRecetas.de(filtro), yo.id(), page, size);
    }

    @GetMapping("/sugerencias")
    public List<SugerenciaRecetaDto> sugerencias(
            @RequestParam(defaultValue = "") @Size(max = 100, message = "Busca con 100 caracteres como mucho")
                    String q) {
        return recetas.sugerencias(q);
    }

    @GetMapping("/mejor-valoradas")
    public List<RecetaResumenDto> mejorValoradas(
            @RequestParam(defaultValue = "3")
                    @Min(value = 1, message = "El límite debe estar entre 1 y 20")
                    @Max(value = 20, message = "El límite debe estar entre 1 y 20")
                    int limit,
            @AuthenticationPrincipal UsuarioAutenticado yo) {
        return recetas.mejorValoradas(yo.id(), limit);
    }

    @GetMapping("/ultimas")
    public List<RecetaResumenDto> ultimas(
            @RequestParam(defaultValue = "3")
                    @Min(value = 1, message = "El límite debe estar entre 1 y 20")
                    @Max(value = 20, message = "El límite debe estar entre 1 y 20")
                    int limit,
            @AuthenticationPrincipal UsuarioAutenticado yo) {
        return recetas.ultimas(yo.id(), limit);
    }

    @GetMapping("/{id}")
    public RecetaDetalleDto detalle(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado yo) {
        return recetas.detalle(id, yo.id(), yo.esAdmin());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecetaDetalleDto> crear(
            @Valid @RequestPart("receta") RecetaRequest datos,
            @RequestPart(name = "fotos", required = false) List<MultipartFile> fotos,
            @AuthenticationPrincipal UsuarioAutenticado yo) {
        RecetaDetalleDto creada = recetas.crear(datos, fotos, yo.id());
        return ResponseEntity.created(URI.create("/api/recetas/" + creada.id())).body(creada);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RecetaDetalleDto editar(
            @PathVariable int id,
            @Valid @RequestPart("receta") RecetaRequest datos,
            @RequestPart(name = "fotos", required = false) List<MultipartFile> fotos,
            @AuthenticationPrincipal UsuarioAutenticado yo) {
        return recetas.editar(id, datos, fotos, yo.id());
    }

    @PatchMapping("/{id}/publicada")
    public RecetaDetalleDto publicar(
            @PathVariable int id,
            @Valid @RequestBody PublicacionRequest datos,
            @AuthenticationPrincipal UsuarioAutenticado yo) {
        return recetas.publicar(id, datos.publicada(), yo.id());
    }

    /** Solo el autor (D-061). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado yo) {
        recetas.eliminar(id, yo.id());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/like")
    public LikesDto darLike(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado yo) {
        return recetas.darLike(id, yo.id());
    }

    @DeleteMapping("/{id}/like")
    public LikesDto quitarLike(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado yo) {
        return recetas.quitarLike(id, yo.id());
    }
}
