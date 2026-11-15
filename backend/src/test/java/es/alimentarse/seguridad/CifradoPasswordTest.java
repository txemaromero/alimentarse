package es.alimentarse.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

/** El coste de bcrypt: 12, y solo el perfil {@code test} puede bajarlo (D-090). */
class CifradoPasswordTest {

    @Test
    void porDefectoEs12() throws Exception {
        MockEnvironment entorno = new MockEnvironment();
        new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"))
                .forEach(entorno.getPropertySources()::addLast);

        assertThat(entorno.getProperty("app.contrasenas.coste-bcrypt", Integer.class))
                .isEqualTo(CifradoPassword.COSTE)
                .isEqualTo(12);
    }

    @ParameterizedTest
    @ValueSource(strings = {"prod", "dev", "e2e"})
    void fueraDeLosTestsNoArrancaConMenos(String perfil) {
        MockEnvironment entorno = new MockEnvironment();
        entorno.setActiveProfiles(perfil);

        assertThatThrownBy(() -> CifradoPassword.coste(11, entorno))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.contrasenas.coste-bcrypt no puede bajar de 12");
    }

    @Test
    void sinPerfilTampoco() {
        assertThatThrownBy(() -> CifradoPassword.coste(4, new MockEnvironment()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void fueraDeLosTestsAdmiteEl12OUnoMayor() {
        MockEnvironment entorno = new MockEnvironment();
        entorno.setActiveProfiles("prod");

        assertThat(CifradoPassword.coste(12, entorno)).isEqualTo(12);
        assertThat(CifradoPassword.coste(13, entorno)).isEqualTo(13);
    }

    @Test
    void elPerfilTestSiPuedeBajarlo() {
        MockEnvironment entorno = new MockEnvironment();
        entorno.setActiveProfiles("test");

        assertThat(CifradoPassword.coste(5, entorno)).isEqualTo(5);
    }
}
