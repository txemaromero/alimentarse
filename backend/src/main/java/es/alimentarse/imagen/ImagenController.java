package es.alimentarse.imagen;

import java.time.Duration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Imágenes de las recetas. Pública: se usa en etiquetas {@code <img>}, que no envían el token. */
@RestController
@RequestMapping("/api/imagenes")
class ImagenController {

    /** Los nombres son UUID que nunca cambian de contenido, ni tampoco sus versiones: se pueden cachear sin límite. */
    private static final CacheControl CACHE =
            CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();

    private final ImagenService imagenes;

    ImagenController(ImagenService imagenes) {
        this.imagenes = imagenes;
    }

    @GetMapping("/{nombre}")
    ResponseEntity<Resource> imagen(@PathVariable String nombre, @RequestParam(required = false) Integer ancho) {
        ImagenService.Imagen imagen = imagenes.cargar(nombre, ancho);
        return ResponseEntity.ok()
                .contentType(imagen.tipo())
                .cacheControl(CACHE)
                .body(imagen.recurso());
    }
}
