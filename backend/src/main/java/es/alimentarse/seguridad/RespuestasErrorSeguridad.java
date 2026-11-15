package es.alimentarse.seguridad;

import es.alimentarse.comun.error.ErrorRespuesta;
import es.alimentarse.comun.error.ExcepcionApi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Errores que se producen en los filtros de seguridad, antes de llegar a los controladores: se devuelven con el mismo
 * formato JSON que {@link es.alimentarse.comun.error.ManejadorErrores}.
 */
@Component
public class RespuestasErrorSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper json;

    RespuestasErrorSeguridad(JsonMapper json) {
        this.json = json;
    }

    @Override
    public void commence(
            HttpServletRequest peticion, HttpServletResponse respuesta, AuthenticationException autenticacion)
            throws IOException {
        // El token era válido, pero la cuenta se ha bloqueado mientras tanto (D-063)
        if (peticion.getAttribute(FiltroJwt.CUENTA_RECHAZADA) == EstadoCuentas.Estado.BLOQUEADA) {
            escribir(
                    respuesta,
                    HttpStatus.UNAUTHORIZED,
                    ErrorRespuesta.de("CUENTA_BLOQUEADA", "Tu cuenta está bloqueada por la administración."));
            return;
        }
        escribir(
                respuesta,
                HttpStatus.UNAUTHORIZED,
                ErrorRespuesta.de("NO_AUTENTICADO", "Tu sesión no es válida o ha caducado. Inicia sesión de nuevo."));
    }

    @Override
    public void handle(HttpServletRequest peticion, HttpServletResponse respuesta, AccessDeniedException acceso)
            throws IOException {
        escribir(
                respuesta,
                HttpStatus.FORBIDDEN,
                ErrorRespuesta.de("ACCESO_DENEGADO", "No tienes permiso para hacer esto."));
    }

    void escribir(HttpServletResponse respuesta, ExcepcionApi error) throws IOException {
        if (error.segundosParaReintentar() != null) {
            respuesta.setHeader(HttpHeaders.RETRY_AFTER, error.segundosParaReintentar());
        }
        escribir(respuesta, error.getEstado(), error.comoRespuesta());
    }

    private void escribir(HttpServletResponse respuesta, HttpStatus estado, ErrorRespuesta cuerpo) throws IOException {
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        json.writeValue(respuesta.getOutputStream(), cuerpo);
    }
}
