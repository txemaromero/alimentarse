package es.alimentarse.catalogo;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class CatalogoController {

    private final CatalogoService catalogo;

    CatalogoController(CatalogoService catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping("/grupos")
    List<GrupoDto> grupos() {
        return catalogo.grupos();
    }

    @GetMapping("/tipos-receta")
    List<TipoRecetaDto> tiposReceta() {
        return catalogo.tiposReceta();
    }
}
