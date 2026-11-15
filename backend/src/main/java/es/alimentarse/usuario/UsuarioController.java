package es.alimentarse.usuario;

import es.alimentarse.seguridad.CookieSesion;
import es.alimentarse.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Perfil del usuario que ha iniciado sesión. */
@RestController
@RequestMapping("/api/usuarios/me")
class UsuarioController {

    private final UsuarioService usuarios;
    private final ConfirmacionEmailService confirmaciones;
    private final CookieSesion cookies;

    UsuarioController(UsuarioService usuarios, ConfirmacionEmailService confirmaciones, CookieSesion cookies) {
        this.usuarios = usuarios;
        this.confirmaciones = confirmaciones;
        this.cookies = cookies;
    }

    @GetMapping
    PerfilDto perfil(@AuthenticationPrincipal UsuarioAutenticado yo) {
        return usuarios.perfil(yo.id());
    }

    @PutMapping
    PerfilDto actualizar(
            @AuthenticationPrincipal UsuarioAutenticado yo, @Valid @RequestBody ActualizarPerfilRequest datos) {
        return usuarios.actualizarPerfil(yo.id(), datos);
    }

    /** Vuelve a enviar el enlace para confirmar el email (D-088). */
    @PostMapping("/email/confirmacion")
    EnlaceEnviadoDto reenviarConfirmacion(@AuthenticationPrincipal UsuarioAutenticado yo) {
        return confirmaciones.reenviar(yo.id());
    }

    /** Descarta el email nuevo que esperaba su confirmación (D-088). */
    @DeleteMapping("/email/pendiente")
    PerfilDto cancelarCambioEmail(@AuthenticationPrincipal UsuarioAutenticado yo) {
        confirmaciones.cancelarCambio(yo.id());
        return usuarios.perfil(yo.id());
    }

    /** Deja de poder entrar con Google; pide la contraseña (D-086). */
    @PostMapping("/desvincular-google")
    PerfilDto desvincularGoogle(
            @AuthenticationPrincipal UsuarioAutenticado yo, @Valid @RequestBody DesvincularGoogleRequest datos) {
        return usuarios.desvincularGoogle(yo.id(), datos);
    }

    @PutMapping("/password")
    ResponseEntity<Void> cambiarPassword(
            @AuthenticationPrincipal UsuarioAutenticado yo, @Valid @RequestBody CambioPasswordRequest datos) {
        String token = usuarios.cambiarPassword(yo.id(), datos);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.crear(token))
                .build();
    }
}
