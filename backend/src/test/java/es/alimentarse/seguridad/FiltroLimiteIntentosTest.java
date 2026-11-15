package es.alimentarse.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/** Qué peticiones cuentan para cada límite de intentos (D-086). */
class FiltroLimiteIntentosTest {

    @Test
    void cadaRutaCuentaEnSuContador() {
        assertThat(contador("POST", "/api/auth/login")).contains("login");
        assertThat(contador("POST", "/api/auth/google/vincular")).contains("password");
        assertThat(contador("PUT", "/api/usuarios/me")).contains("password");
        assertThat(contador("PUT", "/api/usuarios/me/password")).contains("password");
        assertThat(contador("POST", "/api/usuarios/me/desvincular-google")).contains("password");
        assertThat(contador("POST", "/api/usuarios/me/baja")).contains("password");
        assertThat(contador("POST", "/api/auth/password/olvido")).contains("olvido");
        assertThat(contador("POST", "/api/auth/registro")).contains("registro");
        assertThat(contador("POST", "/api/auth/google/registro")).contains("registro");
        assertThat(contador("GET", "/oauth2/authorization/google")).contains("google");
    }

    /** Spring MVC decodifica la ruta: {@code /api/auth/%6Cogin} llega al login y debe contar como él. */
    @Test
    void unaRutaCodificadaCuentaComoLaDecodificada() {
        assertThat(contador("POST", "/api/auth/%6Cogin")).contains("login");
        assertThat(contador("POST", "/api/auth/password/%6Flvido")).contains("olvido");
        assertThat(contador("POST", "/api/auth/%72egistro")).contains("registro");
    }

    @Test
    void elRestoNoCuenta() {
        assertThat(contador("GET", "/api/auth/login")).isEmpty();
        assertThat(contador("GET", "/api/usuarios/me")).isEmpty();
        assertThat(contador("POST", "/api/recetas")).isEmpty();
        assertThat(contador("GET", "/login/oauth2/code/google")).isEmpty();
    }

    private static Optional<String> contador(String metodo, String ruta) {
        return FiltroLimiteIntentos.regla(new MockHttpServletRequest(metodo, ruta))
                .map(FiltroLimiteIntentos.Regla::contador);
    }
}
