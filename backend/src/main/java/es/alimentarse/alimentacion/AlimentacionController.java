package es.alimentarse.alimentacion;

import es.alimentarse.seguridad.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alimentacion")
public class AlimentacionController {

    private final AlimentacionService alimentacion;

    public AlimentacionController(AlimentacionService alimentacion) {
        this.alimentacion = alimentacion;
    }

    @GetMapping("/resumen")
    public ResumenAlimentacionDto resumen(@AuthenticationPrincipal UsuarioAutenticado yo) {
        return alimentacion.resumen(yo.id());
    }
}
