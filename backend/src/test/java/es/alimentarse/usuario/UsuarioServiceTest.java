package es.alimentarse.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.seguridad.JwtService;
import es.alimentarse.support.PropiedadesDePrueba;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    UsuarioMapper usuarios;

    @Mock
    ApplicationEventPublisher eventos;

    @Mock
    ConfirmacionEmailService confirmaciones;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private UsuarioService servicio;
    private Usuario lucia;

    @BeforeEach
    void preparar() {
        servicio = new UsuarioService(
                usuarios,
                passwordEncoder,
                new JwtService(PropiedadesDePrueba.crear(), Clock.systemUTC()),
                eventos,
                confirmaciones);
        lucia = new Usuario(
                "lucia",
                "Lucía",
                "Fernández",
                "Ruiz",
                "lucia@toca.local",
                passwordEncoder.encode("demo1234"),
                null,
                null,
                false,
                null,
                true,
                null);
    }

    @Test
    void elPerfilIncluyeLosRolesYNoLaContrasena() {
        when(usuarios.buscarPorId("lucia")).thenReturn(Optional.of(lucia));
        when(usuarios.rolesDe("lucia")).thenReturn(List.of("USER"));

        assertThat(servicio.perfil("lucia"))
                .isEqualTo(new PerfilDto(
                        "lucia", "Lucía", "Fernández", "Ruiz", "lucia@toca.local", List.of("USER"), false, true, null));
    }

    @Test
    void elPerfilDeUnUsuarioQueYaNoExisteDevuelve404() {
        when(usuarios.buscarPorId("borrado")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.perfil("borrado"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void actualizarConElEmailDeOtraCuentaDevuelve409() {
        when(usuarios.buscarPorId("lucia")).thenReturn(Optional.of(lucia));
        when(usuarios.existeEmail("marcos@toca.local", "lucia")).thenReturn(true);

        assertThatThrownBy(() -> servicio.actualizarPerfil(
                        "lucia",
                        new ActualizarPerfilRequest("Lucía", "Fernández", null, "marcos@toca.local", "demo1234")))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getErrores()).containsOnlyKeys("email");
                });
        verify(usuarios, never()).actualizarPerfil(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void actualizarGuardaLosDatosRecortadosYSinSegundoApellidoVacio() {
        when(usuarios.buscarPorId("lucia")).thenReturn(Optional.of(lucia));

        servicio.actualizarPerfil(
                "lucia", new ActualizarPerfilRequest(" Lucía ", " Fernández ", " ", " lucia@nuevo.es ", "demo1234"));

        // El email nuevo espera su confirmación: la cuenta sigue con el anterior (D-088)
        verify(usuarios).actualizarPerfil("lucia", "Lucía", "Fernández", null, "lucia@toca.local");
    }

    @Test
    void cambiarElEmailEnviaElEnlaceAlNuevoYNoLoUsaHastaQueSeConfirme() {
        when(usuarios.buscarPorId("lucia")).thenReturn(Optional.of(lucia));

        servicio.actualizarPerfil(
                "lucia", new ActualizarPerfilRequest("Lucía M.", "Fernández", "Ruiz", "lucia@nuevo.es", "demo1234"));

        verify(usuarios).actualizarPerfil("lucia", "Lucía M.", "Fernández", "Ruiz", "lucia@toca.local");
        verify(confirmaciones).enviarEnlace("lucia", "Lucía M.", "lucia@toca.local", "lucia@nuevo.es");
        // El aviso al email anterior llega cuando se confirma el nuevo
        verify(usuarios, never()).anularRecuperacion(anyString());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void cambiarElEmailSinLaContrasenaOConOtraNoCambiaNada() {
        when(usuarios.buscarPorId("lucia")).thenReturn(Optional.of(lucia));

        for (String password : new String[] {null, "", "incorrecta"}) {
            assertThatThrownBy(() -> servicio.actualizarPerfil(
                            "lucia",
                            new ActualizarPerfilRequest("Lucía", "Fernández", null, "otro@ejemplo.es", password)))
                    .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                        assertThat(e.getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(e.getCodigo()).isEqualTo("PASSWORD_INCORRECTA");
                        assertThat(e.getErrores())
                                .containsEntry(
                                        "password",
                                        "incorrecta".equals(password)
                                                ? UsuarioService.PASSWORD_ACTUAL_INCORRECTA
                                                : UsuarioService.PASSWORD_PARA_EMAIL);
                    });
        }
        verify(usuarios, never()).actualizarPerfil(anyString(), anyString(), anyString(), any(), anyString());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void sinCambiarElEmailNoHaceFaltaLaContrasenaNiSeAvisa() {
        when(usuarios.buscarPorId("lucia")).thenReturn(Optional.of(lucia));

        // Solo cambian las mayúsculas: es el mismo email
        servicio.actualizarPerfil(
                "lucia", new ActualizarPerfilRequest("Lucía", "Fernández", null, "Lucia@Toca.local", null));

        verify(usuarios).actualizarPerfil("lucia", "Lucía", "Fernández", null, "Lucia@Toca.local");
        verify(confirmaciones, never()).enviarEnlace(any(), any(), any(), any());
        verify(usuarios, never()).anularRecuperacion(anyString());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void desvincularGoogleConLaContrasenaLoQuitaYAvisa() {
        Usuario vinculada = conGoogle("google-lucia");
        when(usuarios.buscarPorId("lucia")).thenReturn(Optional.of(vinculada));

        servicio.desvincularGoogle("lucia", new DesvincularGoogleRequest("demo1234"));

        verify(usuarios).desvincularGoogle("lucia");
        verify(eventos)
                .publishEvent(
                        new AvisoCuenta("Lucía", "lucia@toca.local", AvisoCuenta.Motivo.GOOGLE_DESVINCULADO, null));
    }

    @Test
    void desvincularGoogleSinVincularNoHaceNadaYConOtraContrasenaTampoco() {
        when(usuarios.buscarPorId("lucia")).thenReturn(Optional.of(lucia));
        servicio.desvincularGoogle("lucia", new DesvincularGoogleRequest("demo1234"));

        when(usuarios.buscarPorId("lucia")).thenReturn(Optional.of(conGoogle("google-lucia")));
        assertThatThrownBy(() -> servicio.desvincularGoogle("lucia", new DesvincularGoogleRequest("incorrecta")))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class,
                        e -> assertThat(e.getErrores())
                                .containsEntry("password", UsuarioService.PASSWORD_ACTUAL_INCORRECTA));

        verify(usuarios, never()).desvincularGoogle(anyString());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    private Usuario conGoogle(String googleId) {
        return new Usuario(
                lucia.id(),
                lucia.nombre(),
                lucia.apellido1(),
                lucia.apellido2(),
                lucia.email(),
                lucia.password(),
                null,
                null,
                false,
                googleId,
                true,
                null);
    }

    @Test
    void cambiarPasswordExigeLaContrasenaActual() {
        when(usuarios.buscarPorId("lucia")).thenReturn(Optional.of(lucia));

        assertThatThrownBy(
                        () -> servicio.cambiarPassword("lucia", new CambioPasswordRequest("incorrecta", "nueva-clave")))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getErrores()).containsEntry("actual", UsuarioService.PASSWORD_ACTUAL_INCORRECTA);
                });
        verify(usuarios, never()).actualizarPassword(anyString(), anyString());
    }

    @Test
    void cambiarPasswordGuardaLaNuevaCifrada() {
        when(usuarios.buscarPorId("lucia")).thenReturn(Optional.of(lucia));

        servicio.cambiarPassword("lucia", new CambioPasswordRequest("demo1234", "nueva-clave"));

        ArgumentCaptor<String> cifrada = ArgumentCaptor.forClass(String.class);
        verify(usuarios).actualizarPassword(eq("lucia"), cifrada.capture());
        assertThat(passwordEncoder.matches("nueva-clave", cifrada.getValue())).isTrue();
    }
}
