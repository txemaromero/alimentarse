package es.alimentarse.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import es.alimentarse.auth.RegistroRequest;
import es.alimentarse.auth.RestablecerPasswordRequest;
import es.alimentarse.usuario.CambioPasswordRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Las contraseñas nuevas caben en los 72 bytes de bcrypt, y no solo en 72 caracteres (D-090). */
class CabeEnBcryptTest {

    private static final String DEMASIADO_LARGA =
            "La contraseña es demasiado larga: la ñ, las tildes y los emojis cuentan como más de un carácter";
    private static final String LONGITUD = "La contraseña debe tener entre 8 y 72 caracteres";

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void crearValidador() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        fabrica.close();
    }

    @Test
    void cabeSiOcupaComoMucho72BytesEnUtf8() {
        assertThat(CifradoPassword.cabe("a".repeat(72))).isTrue();
        assertThat(CifradoPassword.cabe("a".repeat(73))).isFalse();
        assertThat(CifradoPassword.cabe("ñ".repeat(36))).isTrue();
        assertThat(CifradoPassword.cabe("ñ".repeat(37))).isFalse();
        // Un emoji ocupa 4 bytes (y 2 caracteres para Java)
        assertThat(CifradoPassword.cabe("😀".repeat(18))).isTrue();
        assertThat(CifradoPassword.cabe("😀".repeat(19))).isFalse();
    }

    /** 40 «ñ» son 40 caracteres, que pasan el {@code @Size}, y 80 bytes, que bcrypt se niega a cifrar. */
    @ParameterizedTest
    @MethodSource("contrasenasNuevas")
    void conMasDe72BytesLoDiceUnaVez(Function<String, Object> peticion, String campo) {
        assertThat(mensajes(peticion.apply("ñ".repeat(40)), campo)).containsExactly(DEMASIADO_LARGA);
    }

    @ParameterizedTest
    @MethodSource("contrasenasNuevas")
    void conMasDe72CaracteresSoloLoDiceElSize(Function<String, Object> peticion, String campo) {
        assertThat(mensajes(peticion.apply("ñ".repeat(73)), campo)).containsExactly(LONGITUD);
    }

    @ParameterizedTest
    @MethodSource("contrasenasNuevas")
    void con72BytesEsValida(Function<String, Object> peticion, String campo) {
        assertThat(mensajes(peticion.apply("ñ".repeat(36)), campo)).isEmpty();
        assertThat(mensajes(peticion.apply("a".repeat(72)), campo)).isEmpty();
    }

    static Stream<Arguments> contrasenasNuevas() {
        return Stream.of(
                Arguments.of(
                        Named.<Function<String, Object>>of(
                                "registro",
                                p -> new RegistroRequest("ana", "Ana", "López", null, "ana@pruebas.local", p)),
                        "password"),
                Arguments.of(
                        Named.<Function<String, Object>>of(
                                "restablecer", p -> new RestablecerPasswordRequest("token", p)),
                        "password"),
                Arguments.of(
                        Named.<Function<String, Object>>of(
                                "cambio de contraseña", p -> new CambioPasswordRequest("actual", p)),
                        "nueva"));
    }

    private static List<String> mensajes(Object peticion, String campo) {
        return validador.validate(peticion).stream()
                .filter(v -> v.getPropertyPath().toString().equals(campo))
                .map(ConstraintViolation::getMessage)
                .toList();
    }
}
