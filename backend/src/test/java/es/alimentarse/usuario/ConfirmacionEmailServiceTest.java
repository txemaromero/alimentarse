package es.alimentarse.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import es.alimentarse.comun.TokenSeguro;
import es.alimentarse.comun.error.ExcepcionApi;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

/** Confirmación del email con un enlace (D-088). */
@ExtendWith(MockitoExtension.class)
class ConfirmacionEmailServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-14T10:00:00Z");

    @Mock
    UsuarioMapper usuarios;

    @Mock
    ApplicationEventPublisher eventos;

    private ConfirmacionEmailService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ConfirmacionEmailService(usuarios, eventos, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    void elEnlaceGuardaSoloElHashDelTokenYCaducaEn24Horas() {
        String token = servicio.prepararEnlace("lucia", "lucia@nuevo.es");

        assertThat(token).hasSizeGreaterThanOrEqualTo(43);
        verify(usuarios)
                .guardarTokenEmail(
                        "lucia",
                        TokenSeguro.sha256(token),
                        AHORA.plus(ConfirmacionEmailService.VALIDEZ),
                        "lucia@nuevo.es");
        assertThat(servicio.prepararEnlace("lucia", null)).isNotEqualTo(token);
    }

    @Test
    void elEnlaceVaAlEmailNuevoSiEsUnCambioYSiNoAlDeLaCuenta() {
        servicio.enviarEnlace("lucia", "Lucía", "lucia@toca.local", "lucia@nuevo.es");
        servicio.enviarEnlace("lucia", "Lucía", "lucia@toca.local", null);

        ArgumentCaptor<ConfirmacionEmailSolicitada> eventosEnviados =
                ArgumentCaptor.forClass(ConfirmacionEmailSolicitada.class);
        verify(eventos, org.mockito.Mockito.times(2)).publishEvent(eventosEnviados.capture());
        assertThat(eventosEnviados.getAllValues())
                .extracting(ConfirmacionEmailSolicitada::email, ConfirmacionEmailSolicitada::cambio)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("lucia@nuevo.es", true),
                        org.assertj.core.groups.Tuple.tuple("lucia@toca.local", false));
    }

    @Test
    void reenviarLoMandaAlEmailPendienteOAlDeLaCuentaSinConfirmar() {
        when(usuarios.buscarPorId("lucia")).thenReturn(Optional.of(usuario(true, "lucia@nuevo.es")));
        assertThat(servicio.reenviar("lucia")).isEqualTo(new EnlaceEnviadoDto("lucia@nuevo.es"));
        verify(usuarios).guardarTokenEmail(eq("lucia"), anyString(), any(), eq("lucia@nuevo.es"));

        when(usuarios.buscarPorId("lucia")).thenReturn(Optional.of(usuario(false, null)));
        assertThat(servicio.reenviar("lucia")).isEqualTo(new EnlaceEnviadoDto("lucia@toca.local"));
        verify(usuarios).guardarTokenEmail(eq("lucia"), anyString(), any(), eq(null));
    }

    @Test
    void conElEmailConfirmadoYSinCambiosNoHayNadaQueReenviar() {
        when(usuarios.buscarPorId("lucia")).thenReturn(Optional.of(usuario(true, null)));

        assertThatThrownBy(() -> servicio.reenviar("lucia")).isInstanceOfSatisfying(ExcepcionApi.class, e -> {
            assertThat(e.getEstado()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(e.getCodigo()).isEqualTo("EMAIL_YA_CONFIRMADO");
        });
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void cancelarElCambioDejaElEmailComoEstaba() {
        when(usuarios.buscarPorId("lucia")).thenReturn(Optional.of(usuario(true, "lucia@nuevo.es")));

        servicio.cancelarCambio("lucia");

        verify(usuarios).cancelarCambioEmail("lucia");
    }

    @Test
    void confirmarSinCambioSoloMarcaElEmailComoConfirmado() {
        when(usuarios.buscarPorTokenEmail(TokenSeguro.sha256("tok"), AHORA))
                .thenReturn(Optional.of(usuario(false, null)));

        assertThat(servicio.confirmar(" tok ")).isEqualTo(new EmailConfirmadoDto("lucia", "lucia@toca.local"));

        verify(usuarios).confirmarEmail("lucia");
        verify(usuarios, never()).anularRecuperacion(anyString());
        verify(usuarios, never()).anularSesiones(anyString());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void confirmarUnCambioUsaElEmailNuevoAnulaLaRecuperacionYAvisaAlAnterior() {
        when(usuarios.buscarPorTokenEmail(TokenSeguro.sha256("tok"), AHORA))
                .thenReturn(Optional.of(usuario(true, "lucia@nuevo.es")));

        assertThat(servicio.confirmar("tok")).isEqualTo(new EmailConfirmadoDto("lucia", "lucia@nuevo.es"));

        verify(usuarios).confirmarEmail("lucia");
        verify(usuarios).anularRecuperacion("lucia");
        // Cierra las sesiones abiertas (D-089)
        verify(usuarios).anularSesiones("lucia");
        verify(eventos)
                .publishEvent(new AvisoCuenta(
                        "Lucía", "lucia@toca.local", AvisoCuenta.Motivo.EMAIL_CAMBIADO, "lucia@nuevo.es"));
    }

    @Test
    void siOtraCuentaYaUsaElEmailNuevoNoSeConfirma() {
        when(usuarios.buscarPorTokenEmail(TokenSeguro.sha256("tok"), AHORA))
                .thenReturn(Optional.of(usuario(true, "ocupado@toca.local")));
        when(usuarios.existeEmail("ocupado@toca.local", "lucia")).thenReturn(true);

        assertThatThrownBy(() -> servicio.confirmar("tok")).isInstanceOfSatisfying(ExcepcionApi.class, e -> {
            assertThat(e.getEstado()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(e.getCodigo()).isEqualTo("EMAIL_EN_USO");
        });
        verify(usuarios, never()).confirmarEmail(anyString());
    }

    @Test
    void unEnlaceQueNoValeOHaCaducadoDa400() {
        when(usuarios.buscarPorTokenEmail(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.confirmar("viejo")).isInstanceOfSatisfying(ExcepcionApi.class, e -> {
            assertThat(e.getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(e.getCodigo()).isEqualTo("TOKEN_INVALIDO");
        });
    }

    @Test
    void sinLaCuentaDa404() {
        when(usuarios.buscarPorId("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.reenviar("nadie"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private static Usuario usuario(boolean verificado, String pendiente) {
        return new Usuario(
                "lucia",
                "Lucía",
                "Fernández",
                null,
                "lucia@toca.local",
                "x",
                null,
                null,
                false,
                null,
                verificado,
                pendiente);
    }
}
