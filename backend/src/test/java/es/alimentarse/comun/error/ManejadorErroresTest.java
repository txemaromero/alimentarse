package es.alimentarse.comun.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.ServletRequestBindingException;

/** Lo que no tiene manejador propio (D-089). */
class ManejadorErroresTest {

    private final ManejadorErrores manejador = new ManejadorErrores();

    @Test
    void unaExcepcionDeSpringConEstado4xxConservaSuEstado() {
        ResponseEntity<ErrorRespuesta> respuesta =
                manejador.errorInesperado(new ServletRequestBindingException("Falta una cabecera"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().codigo()).isEqualTo("PETICION_INCORRECTA");
    }

    @Test
    void siElClienteNoAceptaJsonSoloVaElEstado() {
        ResponseEntity<ErrorRespuesta> respuesta =
                manejador.errorInesperado(new HttpMediaTypeNotAcceptableException(List.of(MediaType.APPLICATION_JSON)));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
        assertThat(respuesta.getBody()).isNull();
    }

    @Test
    void loDemasEsUnErrorInterno() {
        ResponseEntity<ErrorRespuesta> respuesta = manejador.errorInesperado(new IllegalStateException("Fallo"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().codigo()).isEqualTo("ERROR_INTERNO");
    }
}
