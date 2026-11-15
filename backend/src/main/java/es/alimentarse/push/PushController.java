package es.alimentarse.push;

import es.alimentarse.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Avisos push del recordatorio de las comidas planificadas (D-078). */
@RestController
@RequestMapping("/api/push")
public class PushController {

    private final PushService push;

    public PushController(PushService push) {
        this.push = push;
    }

    @GetMapping("/clave")
    public ClavePushDto clave() {
        return push.clave();
    }

    /** Guarda la suscripción de este navegador; si ya existía, la actualiza. */
    @PutMapping("/suscripcion")
    public ResponseEntity<Void> suscribir(
            @Valid @RequestBody SuscripcionPushRequest datos, @AuthenticationPrincipal UsuarioAutenticado yo) {
        push.suscribir(yo.id(), datos);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/suscripcion")
    public ResponseEntity<Void> desuscribir(
            @RequestParam String endpoint, @AuthenticationPrincipal UsuarioAutenticado yo) {
        push.desuscribir(yo.id(), endpoint);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/prueba")
    public PruebaPushDto probar(@AuthenticationPrincipal UsuarioAutenticado yo) {
        return push.probar(yo.id());
    }
}
