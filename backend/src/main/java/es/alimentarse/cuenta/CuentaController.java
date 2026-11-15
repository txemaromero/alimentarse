package es.alimentarse.cuenta;

import es.alimentarse.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Descargar los datos personales y darse de baja (D-064). */
@RestController
@RequestMapping("/api/usuarios/me")
public class CuentaController {

    private final CuentaService cuenta;

    public CuentaController(CuentaService cuenta) {
        this.cuenta = cuenta;
    }

    /** JSON con todos los datos del usuario, como fichero descargable. */
    @GetMapping("/datos")
    public ResponseEntity<DatosPersonalesDto> datos(@AuthenticationPrincipal UsuarioAutenticado yo) {
        ContentDisposition adjunto = ContentDisposition.attachment()
                .filename("alimentarse-" + yo.id() + ".json")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, adjunto.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(cuenta.exportar(yo.id()));
    }

    /** POST y no DELETE: la petición lleva la contraseña en el cuerpo. */
    @PostMapping("/baja")
    public ResponseEntity<Void> baja(
            @AuthenticationPrincipal UsuarioAutenticado yo, @Valid @RequestBody BajaRequest datos) {
        cuenta.darDeBaja(yo.id(), datos.password());
        return ResponseEntity.noContent().build();
    }
}
