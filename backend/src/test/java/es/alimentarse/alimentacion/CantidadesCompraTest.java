package es.alimentarse.alimentacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Suma de las cantidades de la lista de la compra (D-076). */
class CantidadesCompraTest {

    @Test
    void siSoloLoPideUnaComidaLaCantidadVaTalCual() {
        assertThat(total("al gusto")).isEqualTo("al gusto");
        assertThat(total("  200 Gramos ")).isEqualTo("200 Gramos");
    }

    @Test
    void sumaLasCantidadesConLaMismaUnidad() {
        assertThat(total("200 gramos", "300 gramos")).isEqualTo("500 gramos");
        assertThat(total("100 ml", "250 ml")).isEqualTo("350 ml");
        assertThat(total("2", "3")).isEqualTo("5");
    }

    @Test
    void cadaComidaDeLaMismaRecetaCuenta() {
        assertThat(CantidadesCompra.total(List.of(new CantidadCompraDto(1, "Tortilla", "4 unidades", 2))))
                .isEqualTo("8 unidades");
        assertThat(CantidadesCompra.total(List.of(new CantidadCompraDto(1, "Tortilla", "al gusto", 2))))
                .isNull();
    }

    @Test
    void elSingularYElPluralSonLaMismaUnidad() {
        assertThat(total("1 unidad", "3 unidades")).isEqualTo("4 unidades");
        assertThat(total("1 sobre", "2 sobres")).isEqualTo("3 sobres");
        assertThat(total("1 limón", "2 limones")).isEqualTo("3 limones");
        assertThat(total("1 diente de ajo", "2 dientes de ajo")).isEqualTo("3 dientes de ajo");
        assertThat(total("1 nuez", "2 nueces")).isEqualTo("3 nueces");
    }

    @Test
    void siSoloHaySingularElTotalSeEscribeEnPlural() {
        assertThat(total("1 unidad", "1 unidad")).isEqualTo("2 unidades");
        assertThat(total("1 pizca", "1 pizca")).isEqualTo("2 pizcas");
        assertThat(total("1 limón", "1 limón")).isEqualTo("2 limones");
        assertThat(total("1 nuez", "1 nuez")).isEqualTo("2 nueces");
        assertThat(total("1 kg", "1 kg")).isEqualTo("2 kg");
        assertThat(total("1 diente de ajo", "1 diente de ajo")).isEqualTo("2 dientes de ajo");
    }

    @Test
    void unTotalDeUnoVaEnSingular() {
        assertThat(total("0,5 litros", "1/2 litro")).isEqualTo("1 litro");
    }

    @Test
    void entiendeDecimalesFraccionesYMiles() {
        assertThat(total("1,5 litros", "0.5 litros")).isEqualTo("2 litros");
        assertThat(total("1/2 taza", "1 1/2 tazas")).isEqualTo("2 tazas");
        assertThat(total("1.500 gramos", "500 gramos")).isEqualTo("2.000 gramos");
        assertThat(total("1/3 taza", "1/3 taza")).isEqualTo("0,67 tazas");
        assertThat(total("2kg", "1 kg")).isEqualTo("3 kg");
    }

    @Test
    void noSumaUnidadesDistintasNiLoQueNoEmpiezaPorUnNumero() {
        assertThat(total("250 gramos", "1 kg")).isNull();
        assertThat(total("2 dientes", "1 diente de ajo")).isNull();
        assertThat(total("1 pizca", "al gusto")).isNull();
        assertThat(total("la piel de 1 unidad", "1 unidad")).isNull();
        assertThat(total("2-3 dientes", "1 diente")).isNull();
        assertThat(total("1 (grande)", "1")).isNull();
        assertThat(total("1,5.2 litros", "1 litro")).isNull();
        assertThat(total("1/0 taza", "1 taza")).isNull();
    }

    @Test
    void leeElNumeroYLaUnidad() {
        assertThat(CantidadesCompra.leer("  500   Gramos "))
                .contains(new CantidadesCompra.Medida(new BigDecimal("500"), "gramos"));
        assertThat(CantidadesCompra.leer("3")).contains(new CantidadesCompra.Medida(new BigDecimal("3"), ""));
        assertThat(CantidadesCompra.leer("al gusto")).isEmpty();
    }

    @Test
    void pluralDeLaPrimeraPalabra() {
        assertThat(CantidadesCompra.pluralizar("unidad")).isEqualTo("unidades");
        assertThat(CantidadesCompra.pluralizar("cucharada sopera")).isEqualTo("cucharadas sopera");
        assertThat(CantidadesCompra.pluralizar("café")).isEqualTo("cafés");
        assertThat(CantidadesCompra.pluralizar("ml")).isEqualTo("ml");
        assertThat(CantidadesCompra.pluralizar("")).isEmpty();
    }

    private static String total(String... cantidades) {
        List<CantidadCompraDto> lista = new ArrayList<>();
        for (int i = 0; i < cantidades.length; i++) {
            lista.add(new CantidadCompraDto(i + 1, "Receta " + (i + 1), cantidades[i], 1));
        }
        return CantidadesCompra.total(lista);
    }

    @Test
    void losNumerosConLetrasDelPrincipioSeLeenComoNumeros() {
        assertThat(CantidadesCompra.leer("media")).contains(new CantidadesCompra.Medida(new BigDecimal("0.5000"), ""));
        assertThat(CantidadesCompra.leer("Medio kilo"))
                .contains(new CantidadesCompra.Medida(new BigDecimal("0.5000"), "kilo"));
        assertThat(CantidadesCompra.leer("una lata")).contains(new CantidadesCompra.Medida(BigDecimal.ONE, "lata"));
        assertThat(CantidadesCompra.leer("un diente de ajo"))
                .contains(new CantidadesCompra.Medida(BigDecimal.ONE, "diente de ajo"));
        // Solo como primera palabra entera
        assertThat(CantidadesCompra.leer("mediana")).isEmpty();
        assertThat(CantidadesCompra.leer("al gusto")).isEmpty();
    }
}
