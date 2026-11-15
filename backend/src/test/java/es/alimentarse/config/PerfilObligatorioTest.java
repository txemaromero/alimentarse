package es.alimentarse.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.env.MockEnvironment;

/** Sin un perfil explícito, la aplicación no arranca (D-089). */
class PerfilObligatorioTest {

    @Test
    void sinPerfilNoArrancaYDiceQueVariableFalta() {
        assertThatThrownBy(() -> PerfilObligatorio.exigirPerfil(new MockEnvironment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SPRING_PROFILES_ACTIVE");
    }

    @Test
    void conUnPerfilArranca() {
        MockEnvironment entorno = new MockEnvironment();
        entorno.setActiveProfiles("prod");

        assertThatCode(() -> PerfilObligatorio.exigirPerfil(entorno)).doesNotThrowAnyException();
    }

    @Test
    void seCompruebaAntesDeCrearElContexto() {
        assumeTrue(System.getenv("SPRING_PROFILES_ACTIVE") == null, "Con la variable definida, sí hay perfil");
        // Registrado en META-INF/spring.factories: lo recoge cualquier SpringApplication, sin más configuración
        SpringApplication aplicacion = new SpringApplication(Vacia.class);
        aplicacion.setWebApplicationType(WebApplicationType.NONE);

        assertThatThrownBy(() -> aplicacion.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(PerfilObligatorio.SIN_PERFIL);
    }

    @Configuration(proxyBeanMethods = false)
    static class Vacia {}
}
