package es.alimentarse.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import es.alimentarse.support.TestIntegracion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Cabeceras de seguridad de todas las respuestas y endpoint de salud (D-060). */
@TestIntegracion
class CabecerasYSaludTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void todasLasRespuestasLlevanLasCabecerasDeSeguridad() {
        for (String ruta : new String[] {"/", "/recetas/5", "/api/grupos", "/api/auth/login"}) {
            MvcTestResult resultado = mvc.get().uri(ruta).exchange();
            assertThat(resultado.getResponse().getHeader("Content-Security-Policy"))
                    .as(ruta)
                    .isEqualTo(SecurityConfig.POLITICA_CSP)
                    .contains("script-src 'self';")
                    .contains("frame-ancestors 'none'")
                    .doesNotContain("unsafe-eval");
            assertThat(resultado.getResponse().getHeader("Referrer-Policy"))
                    .isEqualTo("strict-origin-when-cross-origin");
            assertThat(resultado.getResponse().getHeader("Permissions-Policy")).contains("camera=()");
            assertThat(resultado.getResponse().getHeader("Cross-Origin-Opener-Policy"))
                    .isEqualTo("same-origin");
            assertThat(resultado.getResponse().getHeader("X-Frame-Options")).isEqualTo("DENY");
            assertThat(resultado.getResponse().getHeader("X-Content-Type-Options"))
                    .isEqualTo("nosniff");
        }
    }

    @Test
    void laSaludEsPublicaYNoDaDetalles() {
        for (String ruta : new String[] {"/api/health", "/api/health/liveness", "/api/health/readiness"}) {
            assertThat(mvc.get().uri(ruta).exchange())
                    .as(ruta)
                    .hasStatusOk()
                    .bodyJson()
                    .isLenientlyEqualTo("{\"status\":\"UP\"}");
        }
        assertThat(mvc.get().uri("/api/health").exchange()).bodyText().doesNotContain("components");
    }

    @Test
    void elRestoDeEndpointsDeActuatorNoEstanExpuestos() {
        // Sin sesión: 401 como cualquier ruta de la API; con ella tampoco existirían (solo se expone health)
        assertThat(mvc.get().uri("/api/env").exchange()).hasStatus(401);
        assertThat(mvc.get().uri("/api/beans").exchange()).hasStatus(401);
    }
}
