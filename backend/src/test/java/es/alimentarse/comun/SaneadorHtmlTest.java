package es.alimentarse.comun;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SaneadorHtmlTest {

    private final SaneadorHtml saneador = new SaneadorHtml();

    @Test
    void quitaScriptsEventosYEnlacesJavascript() {
        String html = saneador.sanear("<p onclick=\"robar()\">Paso 1</p><script>alert(1)</script>"
                + "<a href=\"javascript:robar()\">pulsa</a><img src=x onerror=robar()>");

        assertThat(html).contains("Paso 1").doesNotContain("script", "onclick", "javascript", "onerror", "<img");
    }

    @Test
    void conservaElFormatoBasicoLasListasYLosEnlacesConNofollow() {
        String html = saneador.sanear("<h2>Preparación</h2><ol><li><strong>Bate</strong> los <em>huevos</em></li>"
                + "<li><u>Cuaja</u></li></ol><p>Ver <a href=\"https://ejemplo.es\">la técnica</a></p>");

        assertThat(html)
                .contains("<h2>", "<ol>", "<li>", "<strong>Bate</strong>", "<em>huevos</em>", "<u>Cuaja</u>")
                .contains("href=\"https://ejemplo.es\"")
                .contains("rel=\"nofollow\"");
    }

    @Test
    void quitaLosEstilos() {
        assertThat(saneador.sanear("<p style=\"position:fixed\">Texto</p>")).isEqualTo("<p>Texto</p>");
    }

    @Test
    void detectaSiQuedaTextoVisible() {
        assertThat(SaneadorHtml.tieneTexto("<p>Hola</p>")).isTrue();
        assertThat(SaneadorHtml.tieneTexto("<p> </p><br /><p>&nbsp;</p>")).isFalse();
        assertThat(SaneadorHtml.tieneTexto(saneador.sanear("<script>alert(1)</script>")))
                .isFalse();
        assertThat(SaneadorHtml.tieneTexto(null)).isFalse();
    }
}
