package es.alimentarse.comun.error;

import es.alimentarse.comun.DatosEnLog;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Convierte cualquier error de la API en una {@link ErrorRespuesta} con el estado HTTP adecuado. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    private static final String MENSAJE_VALIDACION = "Revisa los datos del formulario.";
    private static final String VALOR_NO_VALIDO = "Valor no válido";

    @ExceptionHandler(ExcepcionApi.class)
    ResponseEntity<ErrorRespuesta> excepcionApi(ExcepcionApi e) {
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(e.getEstado());
        if (e.segundosParaReintentar() != null) {
            respuesta.header(HttpHeaders.RETRY_AFTER, e.segundosParaReintentar());
        }
        return respuesta.body(e.comoRespuesta());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorRespuesta> cuerpoNoValido(MethodArgumentNotValidException e) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            // Un parámetro que no se puede convertir (p. ej. tipo=abc) trae un mensaje técnico en inglés
            errores.putIfAbsent(
                    error.getField(), error.isBindingFailure() ? VALOR_NO_VALIDO : error.getDefaultMessage());
        }
        return respuesta(HttpStatus.BAD_REQUEST, new ErrorRespuesta("VALIDACION", MENSAJE_VALIDACION, errores));
    }

    /**
     * Supera {@code spring.servlet.multipart.max-file-size} o {@code max-request-size}: los únicos ficheros que se suben
     * son las fotos de las recetas (D-083).
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorRespuesta> ficheroDemasiadoGrande(MaxUploadSizeExceededException e) {
        return respuesta(
                HttpStatus.CONTENT_TOO_LARGE,
                new ErrorRespuesta(
                        "IMAGEN_DEMASIADO_GRANDE",
                        "Cada foto puede ocupar como mucho 5 MB.",
                        Map.of("fotos", "Cada foto puede ocupar como mucho 5 MB")));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorRespuesta> tipoDeContenidoNoAdmitido(HttpMediaTypeNotSupportedException e) {
        return respuesta(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                ErrorRespuesta.de("TIPO_NO_ADMITIDO", "El formato de la petición no es el que espera este recurso."));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ErrorRespuesta> parametrosNoValidos(HandlerMethodValidationException e) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (ParameterValidationResult resultado : e.getParameterValidationResults()) {
            if (resultado instanceof ParameterErrors erroresDeObjeto) {
                // Objeto con campos (p. ej. los filtros de un @ModelAttribute): un error por campo
                erroresDeObjeto
                        .getFieldErrors()
                        .forEach(error -> errores.putIfAbsent(error.getField(), error.getDefaultMessage()));
            } else {
                resultado
                        .getResolvableErrors()
                        .forEach(error -> errores.putIfAbsent(
                                resultado.getMethodParameter().getParameterName(), error.getDefaultMessage()));
            }
        }
        return respuesta(HttpStatus.BAD_REQUEST, new ErrorRespuesta("VALIDACION", MENSAJE_VALIDACION, errores));
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class,
        MissingServletRequestPartException.class,
        MultipartException.class
    })
    ResponseEntity<ErrorRespuesta> peticionIncorrecta(Exception e) {
        return respuesta(
                HttpStatus.BAD_REQUEST,
                ErrorRespuesta.de("PETICION_INCORRECTA", "La petición no tiene el formato esperado."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorRespuesta> accesoDenegado(AccessDeniedException e) {
        return respuesta(
                HttpStatus.FORBIDDEN, ErrorRespuesta.de("ACCESO_DENEGADO", "No tienes permiso para hacer esto."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorRespuesta> rutaInexistente(NoResourceFoundException e) {
        return respuesta(HttpStatus.NOT_FOUND, ErrorRespuesta.de("NO_ENCONTRADO", "No existe ese recurso."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorRespuesta> metodoNoPermitido(HttpRequestMethodNotSupportedException e) {
        return respuesta(
                HttpStatus.METHOD_NOT_ALLOWED,
                ErrorRespuesta.de("METODO_NO_PERMITIDO", "Esa operación no está permitida en este recurso."));
    }

    /** Dos peticiones simultáneas que pasan la comprobación previa y chocan con un UNIQUE de la base de datos. */
    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<ErrorRespuesta> claveDuplicada(DuplicateKeyException e) {
        log.info(
                "Clave duplicada: {}",
                DatosEnLog.restriccion(e.getMostSpecificCause().getMessage()));
        return respuesta(
                HttpStatus.CONFLICT,
                ErrorRespuesta.de("CONFLICTO", "Ya existe un registro con esos datos. Recarga e inténtalo de nuevo."));
    }

    /**
     * Otra restricción de la base de datos, casi siempre una clave ajena: p. ej. un «me gusta» a una receta que su autor
     * elimina en ese mismo instante.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorRespuesta> integridad(DataIntegrityViolationException e) {
        log.warn(
                "Restricción de la base de datos: {}",
                DatosEnLog.restriccion(e.getMostSpecificCause().getMessage()));
        return respuesta(
                HttpStatus.CONFLICT,
                ErrorRespuesta.de("CONFLICTO", "Los datos han cambiado mientras tanto. Recarga e inténtalo de nuevo."));
    }

    /**
     * Lo inesperado de verdad: 500, con la traza en el log. Una excepción de Spring MVC sin manejador propio aquí (por
     * ejemplo, una cabecera {@code Accept} que no se puede atender) ya trae su estado 4xx: se respeta, sin llenar el log
     * de trazas (D-089).
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorRespuesta> errorInesperado(Exception e) {
        if (e instanceof ErrorResponse deSpring && deSpring.getStatusCode().is4xxClientError()) {
            log.debug("Petición rechazada por Spring MVC: {}", e.toString());
            if (e instanceof HttpMediaTypeNotAcceptableException) {
                // El cliente no acepta JSON: un cuerpo de error tampoco se podría escribir
                return ResponseEntity.status(deSpring.getStatusCode()).build();
            }
            return ResponseEntity.status(deSpring.getStatusCode())
                    .body(ErrorRespuesta.de("PETICION_INCORRECTA", "La petición no tiene el formato esperado."));
        }
        log.error("Error no controlado", e);
        return respuesta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorRespuesta.de("ERROR_INTERNO", "Ha ocurrido un error inesperado. Inténtalo de nuevo más tarde."));
    }

    private static ResponseEntity<ErrorRespuesta> respuesta(HttpStatus estado, ErrorRespuesta cuerpo) {
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
