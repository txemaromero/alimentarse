package es.alimentarse.comun;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextoTest {

    @Test
    void opcionalRecortaYConvierteLoVacioEnNull() {
        assertThat(Texto.opcional("  Ruiz ")).isEqualTo("Ruiz");
        assertThat(Texto.opcional("   ")).isNull();
        assertThat(Texto.opcional(null)).isNull();
    }

    @Test
    void losPatronesEscapanLosComodinesDeLike() {
        assertThat(Texto.patronContiene(" 50%_\\ ")).isEqualTo("%50\\%\\_\\\\%");
        assertThat(Texto.patronEmpiezaPor("to")).isEqualTo("to%");
    }

    @Test
    void sinTextoNoHayPatron() {
        assertThat(Texto.patronContiene(" ")).isNull();
        assertThat(Texto.patronEmpiezaPor(null)).isNull();
    }
}
