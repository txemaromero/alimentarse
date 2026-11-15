package es.alimentarse.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.alimentarse.support.PropiedadesDePrueba;
import io.jsonwebtoken.Jwts;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-01T10:00:00Z");
    private static final UsuarioAutenticado LUCIA = new UsuarioAutenticado("lucia", "Lucía", List.of("USER", "ADMIN"));

    private final JwtService jwt = servicio(PropiedadesDePrueba.SECRETO_JWT, AHORA);

    @Test
    void elTokenGeneradoSeValidaYConservaIdNombreYRoles() {
        JwtService.TokenEmitido emitido = jwt.emitir(LUCIA, 4);
        assertThat(jwt.validar(emitido.valor())).contains(new JwtService.SesionToken(LUCIA, 4, emitido.caduca()));
    }

    @Test
    void indicaCuandoCaducaElToken() {
        assertThat(jwt.emitir(LUCIA, 0).caduca()).isEqualTo(AHORA.plus(Duration.ofHours(8)));
    }

    @Test
    void unTokenAnteriorALaVersionDeSesionNoVale() {
        String sinVersion = Jwts.builder()
                .subject("lucia")
                .expiration(Date.from(AHORA.plus(Duration.ofHours(1))))
                .signWith(JwtService.claveDesde(PropiedadesDePrueba.SECRETO_JWT))
                .compact();

        assertThat(jwt.validar(sinVersion)).isEmpty();
    }

    /** Sin el destinatario de las sesiones no es una sesión, aunque lleve versión (D-086). */
    @Test
    void unTokenSinElDestinatarioDeLasSesionesNoVale() {
        String sinDestinatario = Jwts.builder()
                .subject("lucia")
                .claim("ver", 1)
                .expiration(Date.from(AHORA.plus(Duration.ofHours(1))))
                .signWith(JwtService.claveDesde(PropiedadesDePrueba.SECRETO_JWT))
                .compact();

        assertThat(jwt.validar(sinDestinatario)).isEmpty();
    }

    @Test
    void elTokenCaducaALasHorasConfiguradas() {
        String token = jwt.emitir(LUCIA, 0).valor();

        assertThat(servicio(
                                PropiedadesDePrueba.SECRETO_JWT,
                                AHORA.plus(Duration.ofHours(8)).minusSeconds(60))
                        .validar(token))
                .isPresent();
        assertThat(servicio(
                                PropiedadesDePrueba.SECRETO_JWT,
                                AHORA.plus(Duration.ofHours(8)).plusSeconds(60))
                        .validar(token))
                .isEmpty();
    }

    @Test
    void rechazaUnTokenManipulado() {
        String[] partes = jwt.emitir(LUCIA, 0).valor().split("\\.");
        String cargaFalsa = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString("{\"sub\":\"admin\",\"roles\":[\"ADMIN\"]}".getBytes());

        assertThat(jwt.validar(partes[0] + "." + cargaFalsa + "." + partes[2])).isEmpty();
    }

    @Test
    void rechazaUnTokenFirmadoConOtraClave() {
        String otraClave =
                Base64.getEncoder().encodeToString("otra-clave-distinta-de-al-menos-treinta-y-dos-bytes".getBytes());
        String token = servicio(otraClave, AHORA).emitir(LUCIA, 0).valor();

        assertThat(jwt.validar(token)).isEmpty();
    }

    @Test
    void rechazaTextosQueNoSonTokens() {
        assertThat(jwt.validar("esto.no.es-un-jwt")).isEmpty();
        assertThat(jwt.validar("")).isEmpty();
    }

    @Test
    void unSecretoDeMenosDe256BitsImpideArrancar() {
        String corto = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> JwtService.claveDesde(corto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256 bits");
    }

    @Test
    void unSecretoQueNoEstaEnBase64ImpideArrancar() {
        assertThatThrownBy(() -> JwtService.claveDesde("esto no es base64!!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base64");
    }

    private static JwtService servicio(String secreto, Instant ahora) {
        return new JwtService(PropiedadesDePrueba.conJwt(secreto, 8), Clock.fixed(ahora, ZoneOffset.UTC));
    }

    @Test
    void elTokenDelRegistroConGoogleConservaSusDatosYNoEsUnaSesion() {
        JwtService.RegistroGoogle registro =
                new JwtService.RegistroGoogle("google-1", "ana@gmail.com", "Ana", "García");
        String token = jwt.emitirRegistroGoogle(registro);

        assertThat(jwt.validarRegistroGoogle(token)).contains(registro);
        assertThat(jwt.validar(token)).isEmpty();
        // Y una sesión no vale como registro
        assertThat(jwt.validarRegistroGoogle(jwt.emitir(LUCIA, 1).valor())).isEmpty();
        assertThat(jwt.validarRegistroGoogle("no-es-un-token")).isEmpty();
    }

    @Test
    void elRegistroConGoogleCaducaALosQuinceMinutos() {
        String token = jwt.emitirRegistroGoogle(new JwtService.RegistroGoogle("g", "a@gmail.com", "", ""));

        JwtService masTarde = servicio(PropiedadesDePrueba.SECRETO_JWT, AHORA.plus(Duration.ofMinutes(16)));
        assertThat(masTarde.validarRegistroGoogle(token)).isEmpty();
        JwtService antes = servicio(PropiedadesDePrueba.SECRETO_JWT, AHORA.plus(Duration.ofMinutes(14)));
        assertThat(antes.validarRegistroGoogle(token)).isPresent();
    }

    @Test
    void elTokenDeLaVinculacionConGoogleConservaSusDatosYNoValeParaOtraCosa() {
        JwtService.VinculacionGoogle vinculacion =
                new JwtService.VinculacionGoogle("google-1", "lucia", "lucia@gmail.com");
        String token = jwt.emitirVinculacionGoogle(vinculacion);

        assertThat(jwt.validarVinculacionGoogle(token)).contains(vinculacion);
        assertThat(jwt.validar(token)).isEmpty();
        assertThat(jwt.validarRegistroGoogle(token)).isEmpty();
        assertThat(jwt.validarVinculacionGoogle(jwt.emitir(LUCIA, 1).valor())).isEmpty();
        assertThat(jwt.validarVinculacionGoogle(
                        jwt.emitirRegistroGoogle(new JwtService.RegistroGoogle("g", "a@gmail.com", "", ""))))
                .isEmpty();
        JwtService masTarde = servicio(PropiedadesDePrueba.SECRETO_JWT, AHORA.plus(Duration.ofMinutes(16)));
        assertThat(masTarde.validarVinculacionGoogle(token)).isEmpty();
    }
}
