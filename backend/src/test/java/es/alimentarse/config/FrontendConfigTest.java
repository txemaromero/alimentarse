package es.alimentarse.config;

import static org.assertj.core.api.Assertions.assertThat;

import es.alimentarse.config.FrontendConfig.RutasDeAngular;
import es.alimentarse.support.DatosDePrueba;
import es.alimentarse.support.TestIntegracion;
import es.alimentarse.support.TokensDePrueba;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

/** Servir el build de Angular: rutas de la aplicación, ficheros y caché (12). Usa el build de prueba de test/. */
@TestIntegracion
class FrontendConfigTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    TokensDePrueba tokens;

    @Autowired
    DatosDePrueba datos;

    @ParameterizedTest
    @CsvSource({
        "recetas, true",
        "recetas/5, true",
        "admin/usuarios, true",
        "api, false",
        "api/no-existe, false",
        "main-ABCD1234.js, false",
        "icons/no-existe.png, false",
        "carpeta/fichero.txt, false"
    })
    void distingueLasRutasDeAngular(String ruta, boolean esRuta) {
        assertThat(RutasDeAngular.esRutaDeAngular(ruta)).isEqualTo(esRuta);
    }

    @Test
    void laRaizLlevaAIndexHtml() {
        // Reenvío explícito (FrontendConfig.addViewControllers): MockMvc no sigue los reenvíos
        MvcTestResult resultado = mvc.get().uri("/").exchange();
        assertThat(resultado).hasStatusOk();
        assertThat(resultado.getResponse().getForwardedUrl()).isEqualTo("/index.html");
    }

    @Test
    void cualquierRutaDeAngularDevuelveIndexHtmlSinCache() {
        for (String ruta : new String[] {"/recetas", "/recetas/5", "/mi-alimentacion/historico", "/index.html"}) {
            MvcTestResult resultado = mvc.get().uri(ruta).exchange();
            assertThat(resultado).hasStatusOk();
            assertThat(resultado).bodyText().contains("build de prueba");
            assertThat(resultado.getResponse().getHeader(HttpHeaders.CACHE_CONTROL))
                    .contains("no-store");
        }
    }

    @Test
    void losFicherosConHashSeCacheanUnAno() {
        MvcTestResult resultado = mvc.get().uri("/main-ABCD1234.js").exchange();

        assertThat(resultado).hasStatusOk();
        assertThat(resultado).bodyText().contains("main de prueba");
        assertThat(resultado.getResponse().getHeader(HttpHeaders.CACHE_CONTROL))
                .contains("max-age=31536000")
                .contains("immutable");
    }

    @Test
    void elServiceWorkerYLosIconosNoLlevanCacheLarga() {
        for (String ruta : new String[] {"/ngsw-worker.js", "/icons/icono.svg"}) {
            MvcTestResult resultado = mvc.get().uri(ruta).exchange();
            assertThat(resultado).hasStatusOk();
            assertThat(resultado.getResponse().getHeader(HttpHeaders.CACHE_CONTROL))
                    .doesNotContain("immutable");
        }
    }

    @Test
    void unFicheroQueNoExisteDa404YNoIndexHtml() {
        assertThat(mvc.get().uri("/chunk-NOEXISTE.js").exchange()).hasStatus(404);
        assertThat(mvc.get().uri("/icons/no-existe.png").exchange()).hasStatus(404);
    }

    @Test
    @Transactional
    void unaRutaDeLaApiQueNoExisteSigueDando404EnJson() {
        datos.usuario("visitante");
        assertThat(mvc.get()
                        .uri("/api/no-existe")
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("visitante"))
                        .exchange())
                .hasStatus(404)
                .bodyJson()
                .extractingPath("$.codigo")
                .isEqualTo("NO_ENCONTRADO");
    }
}
