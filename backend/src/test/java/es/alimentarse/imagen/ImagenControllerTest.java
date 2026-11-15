package es.alimentarse.imagen;

import static org.assertj.core.api.Assertions.assertThat;

import es.alimentarse.support.TestIntegracion;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@TestIntegracion
class ImagenControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void lasImagenesDeDemostracionSeSirvenSinSesionYConCacheLarga() {
        MvcTestResult resultado =
                mvc.get().uri("/api/imagenes/demo-tortilla.jpg").exchange();

        assertThat(resultado).hasStatusOk().hasContentType(MediaType.IMAGE_JPEG);
        assertThat(resultado.getResponse().getHeader(HttpHeaders.CACHE_CONTROL))
                .contains("max-age=31536000", "public", "immutable");
        assertThat(resultado.getResponse().getContentAsByteArray()).isNotEmpty();
    }

    @Test
    void sePuedePedirUnaVersionReducida() throws Exception {
        MvcTestResult resultado =
                mvc.get().uri("/api/imagenes/demo-tortilla.jpg?ancho=400").exchange();

        assertThat(resultado).hasStatusOk().hasContentType(MediaType.IMAGE_JPEG);
        assertThat(resultado.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).contains("immutable");
        assertThat(ImageIO.read(new ByteArrayInputStream(resultado.getResponse().getContentAsByteArray()))
                        .getWidth())
                .isEqualTo(400);
    }

    @Test
    void unAnchoNoPrevistoEsUnaPeticionIncorrecta() {
        assertThat(mvc.get().uri("/api/imagenes/demo-tortilla.jpg?ancho=123")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(mvc.get().uri("/api/imagenes/demo-tortilla.jpg?ancho=mucho")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unaImagenQueNoExisteDa404ConElFormatoDeError() {
        assertThat(mvc.get().uri("/api/imagenes/no-existe.jpg"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("NO_ENCONTRADO"));
        assertThat(mvc.get().uri("/api/imagenes/fichero.gif")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void noSePuedenLeerFicherosFueraDelDirectorioDeImagenes() {
        int estado = mvc.get()
                .uri("/api/imagenes/..%2F..%2Fpom.xml")
                .exchange()
                .getResponse()
                .getStatus();

        assertThat(estado).isIn(400, 404);
    }
}
