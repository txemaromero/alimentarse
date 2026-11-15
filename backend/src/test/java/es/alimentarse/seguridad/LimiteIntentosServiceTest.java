package es.alimentarse.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.config.AppProperties;
import es.alimentarse.support.RelojMovil;
import es.alimentarse.support.TestIntegracion;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Contra MySQL real: la lógica de la ventana está en la sentencia SQL (D-071). */
@TestIntegracion
@Transactional
class LimiteIntentosServiceTest {

    private static final AppProperties.Limite TRES_POR_MINUTO = new AppProperties.Limite(3, Duration.ofMinutes(1));

    @Autowired
    LimiteIntentosMapper mapper;

    @Autowired
    JdbcTemplate jdbc;

    private final RelojMovil reloj = new RelojMovil(Instant.parse("2026-11-01T10:00:00Z"));
    private LimiteIntentosService limite;

    @BeforeEach
    void crearServicio() {
        limite = new LimiteIntentosService(mapper, reloj);
    }

    @Test
    void permiteHastaElMaximoYBloqueaElSiguienteIndicandoCuantoEsperar() {
        tresIntentos("login:1.1.1.1");
        reloj.avanzar(Duration.ofSeconds(20));

        assertThatThrownBy(() -> limite.registrarIntento("login:1.1.1.1", TRES_POR_MINUTO))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(e.getCodigo()).isEqualTo("DEMASIADOS_INTENTOS");
                    assertThat(e.getReintentarTras()).isEqualTo(Duration.ofSeconds(40));
                });
    }

    @Test
    void alTerminarLaVentanaVuelveAPermitirIntentosYAbreOtra() {
        tresIntentos("login:1.1.1.1");
        reloj.avanzar(Duration.ofMinutes(1));

        assertThatCode(() -> limite.registrarIntento("login:1.1.1.1", TRES_POR_MINUTO))
                .doesNotThrowAnyException();

        assertThat(mapper.ventana("login:1.1.1.1"))
                .isEqualTo(new LimiteIntentosMapper.Ventana(Instant.parse("2026-11-01T10:02:00Z"), 1));
    }

    @Test
    void cadaClaveTieneSuPropioContador() {
        tresIntentos("login:1.1.1.1");

        assertThatCode(() -> limite.registrarIntento("login:2.2.2.2", TRES_POR_MINUTO))
                .doesNotThrowAnyException();
        assertThatCode(() -> limite.registrarIntento("olvido:1.1.1.1", TRES_POR_MINUTO))
                .doesNotThrowAnyException();
    }

    @Test
    void dosServidoresCompartenElContador() {
        LimiteIntentosService otroServidor = new LimiteIntentosService(mapper, reloj);
        limite.registrarIntento("login:1.1.1.1", TRES_POR_MINUTO);
        otroServidor.registrarIntento("login:1.1.1.1", TRES_POR_MINUTO);
        limite.registrarIntento("login:1.1.1.1", TRES_POR_MINUTO);

        assertThatThrownBy(() -> otroServidor.registrarIntento("login:1.1.1.1", TRES_POR_MINUTO))
                .isInstanceOf(ExcepcionApi.class);
    }

    @Test
    void unaClaveDemasiadoLargaSeRecorta() {
        String larga = "login:" + "x".repeat(300);

        assertThatCode(() -> limite.registrarIntento(larga, TRES_POR_MINUTO)).doesNotThrowAnyException();

        assertThat(mapper.ventana(larga.substring(0, LimiteIntentosService.LONGITUD_MAXIMA_CLAVE)))
                .isNotNull();
    }

    @Test
    void borraPeriodicamenteLasVentanasTerminadas() {
        limite.registrarIntento("antigua", TRES_POR_MINUTO);
        reloj.avanzar(Duration.ofMinutes(2));

        for (int i = 0; i < LimiteIntentosService.INTENTOS_ENTRE_LIMPIEZAS; i++) {
            limite.registrarIntento("reciente", new AppProperties.Limite(100_000, Duration.ofMinutes(1)));
        }

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM limite_intentos WHERE CLAVE = 'antigua'", Long.class))
                .isZero();
        assertThat(mapper.ventana("reciente")).isNotNull();
    }

    /** Los límites que solo cuentan los fallos (D-090): comprobar no suma, y el máximo de fallos ya agota la clave. */
    @Test
    void conLosFallosSeAgotaAlLlegarAlMaximoYComprobarNoCuenta() {
        assertThat(limite.agotado("login-cuenta:ana", TRES_POR_MINUTO)).isZero();
        limite.registrarFallo("login-cuenta:ana", TRES_POR_MINUTO);
        limite.registrarFallo("login-cuenta:ana", TRES_POR_MINUTO);
        assertThat(limite.agotado("login-cuenta:ana", TRES_POR_MINUTO)).isZero();
        assertThat(limite.agotado("login-cuenta:ana", TRES_POR_MINUTO)).isZero();

        limite.registrarFallo("login-cuenta:ana", TRES_POR_MINUTO);
        reloj.avanzar(Duration.ofSeconds(20));

        assertThat(limite.agotado("login-cuenta:ana", TRES_POR_MINUTO)).isEqualTo(Duration.ofSeconds(40));
        assertThat(limite.agotado("login-cuenta:otra", TRES_POR_MINUTO)).isZero();
    }

    @Test
    void losFallosDejanDeContarAlTerminarLaVentana() {
        for (int i = 0; i < 3; i++) {
            limite.registrarFallo("login-cuenta:ana", TRES_POR_MINUTO);
        }
        reloj.avanzar(Duration.ofMinutes(1));

        assertThat(limite.agotado("login-cuenta:ana", TRES_POR_MINUTO)).isZero();
    }

    @Test
    void olvidarPoneLaClaveACero() {
        for (int i = 0; i < 3; i++) {
            limite.registrarFallo("login-cuenta:ana", TRES_POR_MINUTO);
        }

        limite.olvidar("login-cuenta:ana");

        assertThat(limite.agotado("login-cuenta:ana", TRES_POR_MINUTO)).isZero();
        assertThat(mapper.ventana("login-cuenta:ana")).isNull();
    }

    private void tresIntentos(String clave) {
        for (int i = 0; i < 3; i++) {
            limite.registrarIntento(clave, TRES_POR_MINUTO);
        }
    }
}
