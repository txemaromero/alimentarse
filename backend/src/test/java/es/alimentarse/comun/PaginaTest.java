package es.alimentarse.comun;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PaginaTest {

    @Test
    void elTotalDePaginasRedondeaHaciaArriba() {
        assertThat(Pagina.de(List.of(), 0, 16, 77).totalPaginas()).isEqualTo(5);
        assertThat(Pagina.de(List.of(), 0, 16, 64).totalPaginas()).isEqualTo(4);
        assertThat(Pagina.de(List.of(), 0, 9, 1).totalPaginas()).isEqualTo(1);
    }

    @Test
    void sinElementosNoHayPaginas() {
        assertThat(Pagina.de(List.of(), 0, 16, 0).totalPaginas()).isZero();
    }

    @Test
    void elDesplazamientoEsPaginaPorTamano() {
        assertThat(Pagina.desplazamiento(3, 16)).isEqualTo(48);
    }
}
